/* Huezoo panel artwork, built from the app's theme: the #0D0D16 canvas, the
   AmbientGlowBackground corner glows in each game's identity colour, and one hero
   element, the six-petal swatch flower (PetalShape) with one petal a shade off. */
function drawArt(ctx, cfg) {
  const { W, H, dsf, colors: c, index } = cfg;
  const landscape = cfg.mode === 'landscape';
  const k = cfg.copy;

  let seed = 2166136261;
  for (const ch of cfg.slug) seed = Math.imul(seed ^ ch.charCodeAt(0), 16777619) >>> 0;
  const rng = () => {
    seed = (seed + 0x6D2B79F5) >>> 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  const rgba = (hex, a) => {
    const n = parseInt(hex.slice(1), 16);
    return `rgba(${n >> 16},${(n >> 8) & 255},${n & 255},${a})`;
  };
  const mix = (a, b, t) => {
    const x = parseInt(a.slice(1), 16), y = parseInt(b.slice(1), 16);
    const ch = s => Math.round(((x >> s) & 255) * (1 - t) + ((y >> s) & 255) * t);
    return '#' + ((1 << 24) | (ch(16) << 16) | (ch(8) << 8) | ch(0)).toString(16).slice(1);
  };
  const BG = '#0D0D16';

  ctx.fillStyle = BG; ctx.fillRect(0, 0, W, H);
  const glow = (x, y, r, colour, alpha) => {
    const g = ctx.createRadialGradient(x, y, 0, x, y, r);
    g.addColorStop(0, rgba(colour, alpha)); g.addColorStop(0.5, rgba(colour, alpha * 0.36)); g.addColorStop(1, rgba(colour, 0));
    ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
  };
  const wide = !landscape && W > H;  // Duo inner: stacked copy on an almost 4:3 sheet
  const R = landscape || wide ? H : W;
  const right = index % 2 === 0;
  ctx.save(); ctx.globalCompositeOperation = 'screen';
  glow(right ? 0 : W, 0, R * 1.05, c.primary, 0.26);          // top-start
  glow(right ? W : 0, H, R * 1.10, c.secondary, 0.22);        // bottom-end
  ctx.restore();

  // Hero: one large flower leaving the frame on one side.
  const cx = landscape ? W * (right ? 0.98 : 0.40) : wide ? W * (right ? 0.91 : 0.09) : W * (right ? 0.97 : 0.03);
  const cy = landscape ? H * (right ? 0.10 : 0.96) : wide ? H * 0.52 : H * (right ? 0.425 : 0.445);
  const r = R * (landscape ? 0.60 : 0.60);
  const rot = right ? 14 : -14;
  const odd = right ? 4 : 2;  // the petal that is a shade off, kept in view
  const petal = new Path2D('M0 0C45 15 50 60 0 100C-50 60 -45 15 0 0Z');
  const bloom = (blur, alpha) => {
    ctx.save(); ctx.filter = `blur(${blur * dsf}px)`; ctx.globalCompositeOperation = 'screen';
    for (let i = 0; i < 6; i++) {
      ctx.save(); ctx.translate(cx, cy); ctx.rotate((Math.PI / 3) * i + (rot * Math.PI) / 180);
      ctx.translate(0, r * 0.14); ctx.scale(r / 100, r / 100);
      ctx.globalAlpha = alpha; ctx.fillStyle = c.flower; ctx.fill(petal); ctx.restore();
    }
    ctx.restore();
  };
  bloom(R * 0.085, 0.50);
  for (let i = 0; i < 6; i++) {
    const tone = i === odd ? c.flowerOdd : c.flower;
    ctx.save();
    ctx.translate(cx, cy); ctx.rotate((Math.PI / 3) * i + (rot * Math.PI) / 180);
    ctx.translate(0, r * 0.14); ctx.scale(r / 100, r / 100);
    // Deep at the heart, full colour at the rounded end.
    const g = ctx.createLinearGradient(-20, 0, 22, 102);
    g.addColorStop(0, rgba(mix(tone, BG, 0.74), 0.92));
    g.addColorStop(0.55, rgba(mix(tone, BG, 0.28), 0.94));
    g.addColorStop(1, rgba(tone, 0.96));
    ctx.fillStyle = g; ctx.fill(petal);
    // SwatchGradientOverlay: a soft sheen across the upper side.
    ctx.save(); ctx.clip(petal);
    const sheen = ctx.createLinearGradient(-42, 20, 26, 78);
    sheen.addColorStop(0, 'rgba(255,255,255,0.20)'); sheen.addColorStop(0.52, 'rgba(255,255,255,0.02)'); sheen.addColorStop(1, 'rgba(0,0,0,0.16)');
    ctx.fillStyle = sheen; ctx.fillRect(-60, -5, 120, 115);
    ctx.restore();
    const rim = ctx.createLinearGradient(-40, 10, 40, 100);
    rim.addColorStop(0, 'rgba(255,255,255,0.04)'); rim.addColorStop(0.7, 'rgba(255,255,255,0.20)'); rim.addColorStop(1, 'rgba(255,255,255,0.58)');
    ctx.strokeStyle = rim; ctx.lineWidth = 100 / r * 1.2; ctx.stroke(petal);
    ctx.restore();
  }

  // Sink the artwork behind the headline so the type sits on calm ground.
  if (landscape) {
    const g = ctx.createLinearGradient(0, 0, W * 0.52, 0);
    g.addColorStop(0, rgba(BG, 0.82)); g.addColorStop(0.70, rgba(BG, 0.62)); g.addColorStop(1, rgba(BG, 0));
    ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
  } else {
    ctx.save();
    ctx.translate(k.x + k.w / 2, k.y + k.h * 0.52); ctx.scale(1, (k.h * 0.88) / (k.w * 0.74));
    const veil = ctx.createRadialGradient(0, 0, 0, 0, 0, k.w * 0.74);
    veil.addColorStop(0, rgba(BG, 0.74)); veil.addColorStop(0.62, rgba(BG, 0.48)); veil.addColorStop(1, rgba(BG, 0));
    ctx.fillStyle = veil; ctx.fillRect(-W, -H, W * 2, H * 2);
    ctx.restore();
  }

  // Light spilling from the device onto the canvas.
  const d = cfg.device;
  ctx.save(); ctx.globalCompositeOperation = 'screen';
  ctx.translate(d.x + d.w / 2, d.y + d.h * 0.55); ctx.scale(1, 1.25);
  const spill = ctx.createRadialGradient(0, 0, 0, 0, 0, d.w * 0.80);
  spill.addColorStop(0, rgba(c.primary, 0.20)); spill.addColorStop(0.65, rgba(c.primary, 0.09)); spill.addColorStop(1, rgba(c.primary, 0));
  ctx.fillStyle = spill; ctx.fillRect(-W, -H, W * 2, H * 2);
  ctx.restore();

  // Fine grain keeps the dark glows from banding (2 px, so PNGs stay small).
  const grain = document.createElement('canvas'); grain.width = grain.height = 384;
  const gx = grain.getContext('2d'), gi = gx.createImageData(384, 384);
  for (let i = 0; i < gi.data.length; i += 4) {
    const v = rng() < 0.5 ? 0 : 255;
    gi.data[i] = gi.data[i + 1] = gi.data[i + 2] = v; gi.data[i + 3] = Math.floor(rng() * 9);
  }
  gx.putImageData(gi, 0, 0);
  ctx.save(); ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.imageSmoothingEnabled = false; ctx.scale(2, 2);
  ctx.fillStyle = ctx.createPattern(grain, 'repeat'); ctx.fillRect(0, 0, W * dsf / 2, H * dsf / 2); ctx.restore();
}
