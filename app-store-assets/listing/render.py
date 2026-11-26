#!/usr/bin/env python3
"""Render App Store / Play store panels from raw native captures.

Layers, back to front: app artwork painted on a canvas (art.js), optional small
seal, two-line headline, subline, upright device with a slim metal frame.
The same file serves every app; everything app-specific lives in listing.json,
art.js and art.css next to it.

    python3 render.py listing.json                    # every class that has raws
    python3 render.py listing.json --class ipad-13    # one class
    python3 render.py listing.json --out samples      # write somewhere else
    python3 render.py listing.json --verify           # check size, alpha, copy

Reads  <listing dir>/<raw_dirs[class]>/<panel.raw>   (native, never resized)
Writes <listing dir>/<out>/<class>/<NN>_<slug>.png   (exact store pixels, RGB)

A class is skipped, loudly, when any of its raw files is missing.
"""
import argparse
import html
import json
import pathlib
import subprocess
import sys
import tempfile

CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

# px = exact store pixels. css * dsf == px. Headless Chrome clamps a window to a
# 500 css px minimum width, so every css width stays >= 500 and dsf absorbs it.
# zone = share of the height (portrait) or width (landscape) given to the copy.
CLASSES = {
    "iphone-6.9":                 dict(px=(1320, 2868), css=(660, 1434), dsf=2, mode="portrait", zone=0.265, corner=0.125, bezel=0.022),
    "iphone-6.5":                 dict(px=(1242, 2688), css=(621, 1344), dsf=2, mode="portrait", zone=0.265, corner=0.115, bezel=0.022),
    "iphone-6.3":                 dict(px=(1206, 2622), css=(603, 1311), dsf=2, mode="portrait", zone=0.265, corner=0.125, bezel=0.022),
    "ipad-13":                    dict(px=(2064, 2752), css=(516, 688),  dsf=4, mode="portrait", zone=0.255, corner=0.030, bezel=0.026),
    "iphone-duo-outer":           dict(px=(1398, 2034), css=(699, 1017), dsf=2, mode="portrait", zone=0.265, corner=0.080, bezel=0.022),
    "iphone-duo-inner-landscape": dict(px=(2853, 2007), css=(951, 669),  dsf=3, mode="portrait", zone=0.295, corner=0.045, bezel=0.022),  # stacked, like KRAIL
    "iphone-duo-inner-portrait":  dict(px=(2007, 2853), css=(669, 951),  dsf=3, mode="portrait", zone=0.25,  corner=0.045, bezel=0.022),
    "android-phone":              dict(px=(1080, 1920), css=(540, 960),  dsf=2, mode="portrait", zone=0.29,  corner=0.095, bezel=0.024),
    "android-tablet":             dict(px=(1920, 1080), css=(960, 540),  dsf=2, mode="landscape", zone=0.37, corner=0.040, bezel=0.028),
}

PAGE = """<!doctype html><html><head><meta charset="utf-8"><style>
{font_faces}
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
html, body {{ width: {W}px; height: {H}px; overflow: hidden; }}
body {{ -webkit-font-smoothing: antialiased; }}
.panel {{ position: relative; width: {W}px; height: {H}px; overflow: hidden; {vars} }}
#art {{ position: absolute; left: 0; top: 0; width: {W}px; height: {H}px; }}
.copy {{
  position: absolute; z-index: 2; left: {tx}px; top: {ty}px; width: {tw}px; height: {th}px;
  display: flex; flex-direction: column; justify-content: {valign}; align-items: {halign};
  text-align: {talign};
}}
.copy h1 {{ font-size: 100px; }}
.copy h1 span {{ display: block; white-space: nowrap; }}
.copy p {{ text-wrap: balance; }}
.device {{ position: absolute; z-index: 2; left: {dx}px; top: {dy}px; width: {dw}px; height: {dh}px;
  border-radius: {radius}px; padding: {rim}px; }}
.device .bezel {{ width: 100%; height: 100%; padding: {bezel}px; border-radius: {mid_radius}px; background: #07070A; }}
.device img {{ display: block; width: 100%; height: 100%; border-radius: {inner_radius}px; object-fit: fill; }}
{art_css}
</style></head><body><div class="panel {mode}">
<canvas id="art" width="{pxw}" height="{pxh}"></canvas>
<div class="copy" id="copy">{seal}<h1 id="head"><span class="l1">{line1}</span><span class="l2">{line2}</span></h1><p id="sub">{sub}</p></div>
<div class="device"><div class="bezel"><img src="{img_url}"></div></div>
</div>
<script>{art_js}</script>
<script>
const cfg = {art_cfg};
const ctx = document.getElementById('art').getContext('2d');
ctx.scale(cfg.dsf, cfg.dsf);
drawArt(ctx, cfg);
// Largest headline size where both lines fit unwrapped and the block fits the copy box.
document.fonts.ready.then(() => {{
  const copy = document.getElementById('copy'), head = document.getElementById('head'),
        sub = document.getElementById('sub');
  for (let size = {max_font}; size > {min_font}; size -= 0.5) {{
    head.style.fontSize = size + 'px';
    const wide = Math.max(...[...head.children].map(s => s.scrollWidth));
    const tall = sub.getBoundingClientRect().bottom - copy.firstElementChild.getBoundingClientRect().top;
    if (wide <= copy.clientWidth + 0.5 && tall <= copy.clientHeight + 0.5) break;
  }}
}});
</script></body></html>"""


def is_squat(cls):
    W, H = cls["css"]
    return cls["mode"] == "portrait" and H / W < 1.6


def layout(cls, raw_w, raw_h):
    """All geometry in css px for the class canvas. The device stands upright."""
    W, H = cls["css"]
    ratio = raw_h / raw_w
    rim_share = 0.0065
    # Frame thickness is a share of the screen's short side; solve for the screen width.
    short = min(1.0, ratio)
    edge = 2 * (cls["bezel"] + rim_share) * short
    out = {}
    if cls["mode"] == "portrait":
        zone_h = cls["zone"] * H
        top = 0.034 * H
        out.update(tx=0.07 * W, tw=0.86 * W, ty=top, th=zone_h - top,
                   valign="center", halign="center", talign="center")
        gap, bottom = 0.022 * H, 0.04 * H
        avail_h = H - zone_h - gap - bottom
        sw = min(avail_h / (ratio + edge), 0.78 * W / (1 + edge))
    else:
        text_w = cls["zone"] * W
        border = 0.09 * H
        out.update(tx=border, tw=text_w - border, ty=0.14 * H, th=0.72 * H,
                   valign="center", halign="flex-start", talign="left")
        right_w = W - text_w - 0.075 * W
        sw = min(0.80 * H / (ratio + edge), right_w / (1 + edge))
    sh = sw * ratio
    dw, dh = sw + edge * sw, sh + edge * sw
    if cls["mode"] == "portrait":
        out.update(dx=(W - dw) / 2, dy=zone_h + gap + (avail_h - dh) / 2)
    else:
        out.update(dx=text_w + 0.025 * W + (right_w - dw) / 2, dy=(H - dh) / 2)
    side = sw * short
    out.update(dw=dw, dh=dh, rim=rim_share * side, bezel=cls["bezel"] * side, inner_radius=cls["corner"] * side)
    out["mid_radius"] = out["inner_radius"] + out["bezel"]
    out["radius"] = out["mid_radius"] + out["rim"]
    return out


def font_faces(app, root):
    faces = []
    for face in app["fonts"]:
        url = (root / face["file"]).resolve().as_uri()
        faces.append(f'@font-face {{ font-family: "{face["family"]}"; src: url("{url}"); '
                     f'font-weight: {face.get("weight", 400)}; font-style: {face.get("style", "normal")}; }}')
    return "\n".join(faces)


def render_one(root, app, cls_id, cls, panel, index, raw_path, out_path, work):
    from PIL import Image
    with Image.open(raw_path) as im:
        raw_w, raw_h = im.size
    g = layout(cls, raw_w, raw_h)
    W, H = cls["css"]
    # u = the unit art.css sizes decorations in: 1% of the panel's short side.
    u = min(W, H) / 100
    css_vars = f"--u:{u}px;" + "".join(f"--{k}:{v};" for k, v in panel["colors"].items())
    art_cfg = dict(W=W, H=H, dsf=cls["dsf"], mode=cls["mode"], index=index, slug=panel["slug"],
                   colors=panel["colors"], art=panel.get("art", {}),
                   device=dict(x=g["dx"], y=g["dy"], w=g["dw"], h=g["dh"]),
                   copy=dict(x=g["tx"], y=g["ty"], w=g["tw"], h=g["th"]))
    max_font = app["max_font"][cls["mode"]] * (W if cls["mode"] == "portrait" else H) / 100
    page = PAGE.format(
        W=W, H=H, pxw=cls["px"][0], pxh=cls["px"][1], mode=cls["mode"] + (" squat" if is_squat(cls) else ""), vars=css_vars,
        font_faces=font_faces(app, root),
        art_css=(root / "art.css").read_text(encoding="utf-8"),
        art_js=(root / "art.js").read_text(encoding="utf-8"),
        art_cfg=json.dumps(art_cfg),
        seal=f'<span class="seal"><b>{html.escape(panel["seal"])}</b></span>' if panel.get("seal") else "",
        line1=html.escape(panel["lines"][0]), line2=html.escape(panel["lines"][1]),
        sub=html.escape(panel["sub"]), img_url=raw_path.resolve().as_uri(),
        max_font=max_font, min_font=2 * u, **g)
    html_path = work / f"{cls_id}_{out_path.stem}.html"
    html_path.write_text(page, encoding="utf-8")
    out_path.parent.mkdir(parents=True, exist_ok=True)
    tmp_png = work / f"{cls_id}_{out_path.stem}.png"
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--no-sandbox",
                    "--allow-file-access-from-files",
                    f"--force-device-scale-factor={cls['dsf']}", f"--window-size={W},{H}",
                    "--virtual-time-budget=6000", f"--screenshot={tmp_png}", html_path.as_uri()],
                   check=True, capture_output=True, timeout=180)
    with Image.open(tmp_png) as im:
        rgb = im.convert("RGB")
        if rgb.size != tuple(cls["px"]):
            sys.exit(f"{out_path}: rendered {rgb.size}, expected {cls['px']}")
        rgb.save(out_path, optimize=True)


def raw_for(root, app, cls_id, panel):
    d = app["raw_dirs"].get(cls_id)
    return (root / d / panel["raw"]) if d else None


def verify(root, app, out):
    from PIL import Image
    bad = 0
    for panel in app["panels"]:
        words = len(" ".join(panel["lines"]).split())
        if words > 7 or "." in "".join(panel["lines"]):
            print(f"COPY  {panel['slug']}: {words} words / contains a full stop"); bad += 1
    for cls_id, cls in CLASSES.items():
        d = root / out / cls_id
        if not d.exists():
            continue
        files = sorted(d.glob("*.png"))
        if not 1 <= len(files) <= 10:
            print(f"COUNT {cls_id}: {len(files)} files"); bad += 1
        for f in files:
            with Image.open(f) as im:
                if im.size != tuple(cls["px"]) or im.mode != "RGB":
                    print(f"SPEC  {f}: {im.size} {im.mode}, want {cls['px']} RGB"); bad += 1
            if f.stat().st_size > 8 * 1024 * 1024:
                print(f"SIZE  {f}: over 8 MB"); bad += 1
        print(f"ok    {cls_id}: {len(files)} files")
    sys.exit(1 if bad else 0)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("config")
    ap.add_argument("--class", dest="only")
    ap.add_argument("--out", default="store-images")
    ap.add_argument("--verify", action="store_true")
    args = ap.parse_args()
    cfg = pathlib.Path(args.config).resolve()
    root = cfg.parent
    app = json.loads(cfg.read_text())
    if args.verify:
        verify(root, app, args.out)
    with tempfile.TemporaryDirectory(prefix="listing-") as tmp:
        work = pathlib.Path(tmp)
        for cls_id, cls in CLASSES.items():
            if args.only and cls_id != args.only:
                continue
            raws = [raw_for(root, app, cls_id, p) for p in app["panels"]]
            missing = [r for r in raws if r is None or not r.exists()]
            if missing:
                why = f" (e.g. {missing[0]})" if missing[0] else " (no raw dir configured)"
                print(f"skip  {cls_id}: {len(missing)} raw file(s) missing{why}")
                continue
            out_dir = root / args.out / cls_id
            for i, (panel, raw) in enumerate(zip(app["panels"], raws)):
                out = out_dir / f"{i + 1:02d}_{panel['slug']}.png"
                render_one(root, app, cls_id, cls, panel, i, raw, out, work)
                print(f"wrote {out.relative_to(root)}", flush=True)


if __name__ == "__main__":
    main()
