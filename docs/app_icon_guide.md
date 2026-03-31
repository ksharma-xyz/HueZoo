# App Icon Guide — Huezoo

All icons should use the Huezoo visual identity: dark background (`#0D0D16`), neon accent, no transparency on Android, rounded corners handled by the OS.

---

## Android

### Adaptive Icon (API 26+)
Android uses **adaptive icons** — two layers that the OS composites and clips to the device's icon shape.

**Files to create:**

| File | Path | Size | Notes |
|------|------|------|-------|
| `ic_launcher_foreground.xml` or `.png` | `androidApp/src/main/res/mipmap-*/` | See below | Foreground layer — your actual icon art |
| `ic_launcher_background.xml` or `.png` | Same | 108×108 dp | Background layer — solid `#0D0D16` |
| `ic_launcher.xml` | `res/mipmap-anydpi-v26/` | XML manifest | References foreground + background layers |
| Legacy `.png` fallback | `res/mipmap-*/` | See below | For API < 26 |

**Required PNG sizes (foreground + legacy):**

| Density | Folder | Size |
|---------|--------|------|
| mdpi | `mipmap-mdpi` | 48×48 px |
| hdpi | `mipmap-hdpi` | 72×72 px |
| xhdpi | `mipmap-xhdpi` | 96×96 px |
| xxhdpi | `mipmap-xxhdpi` | 144×144 px |
| xxxhdpi | `mipmap-xxxhdpi` | 192×192 px |
| Play Store listing | — | **512×512 px** (PNG, no alpha) |

**Foreground canvas:** Design on 108×108 dp but keep all art within the safe zone — a centered 72×72 dp circle. The OS clips outside this zone on most shapes.

**Quick steps:**
1. Export your icon art as PNG at each size above.
2. Place in the appropriate `mipmap-*` folder under `androidApp/src/main/res/`.
3. In `AndroidManifest.xml` confirm: `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`.
4. Upload the 512×512 PNG to Google Play Console → Store listing → App icon.

---

## iOS

iOS uses a single image set in the Xcode asset catalog.

**File location:** `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/`

**Required sizes:**

| Usage | Size | Scale | Filename (suggested) |
|-------|------|-------|----------------------|
| iPhone notification | 20×20 pt | @2x = 40×40 px | `icon-20@2x.png` |
| iPhone notification | 20×20 pt | @3x = 60×60 px | `icon-20@3x.png` |
| iPhone settings | 29×29 pt | @2x = 58×58 px | `icon-29@2x.png` |
| iPhone settings | 29×29 pt | @3x = 87×87 px | `icon-29@3x.png` |
| iPhone spotlight | 40×40 pt | @2x = 80×80 px | `icon-40@2x.png` |
| iPhone spotlight | 40×40 pt | @3x = 120×120 px | `icon-40@3x.png` |
| iPhone app | 60×60 pt | @2x = 120×120 px | `icon-60@2x.png` |
| iPhone app | 60×60 pt | @3x = 180×180 px | `icon-60@3x.png` |
| iPad notification | 20×20 pt | @1x = 20×20 px | `icon-ipad-20@1x.png` |
| iPad notification | 20×20 pt | @2x = 40×40 px | `icon-ipad-20@2x.png` |
| iPad settings | 29×29 pt | @1x = 29×29 px | `icon-ipad-29@1x.png` |
| iPad settings | 29×29 pt | @2x = 58×58 px | `icon-ipad-29@2x.png` |
| iPad spotlight | 40×40 pt | @1x = 40×40 px | `icon-ipad-40@1x.png` |
| iPad spotlight | 40×40 pt | @2x = 80×80 px | `icon-ipad-40@2x.png` |
| iPad app | 76×76 pt | @1x = 76×76 px | `icon-ipad-76@1x.png` |
| iPad app | 76×76 pt | @2x = 152×152 px | `icon-ipad-76@2x.png` |
| iPad Pro app | 83.5×83.5 pt | @2x = 167×167 px | `icon-ipad-83@2x.png` |
| App Store | 1024×1024 pt | @1x = 1024×1024 px | `icon-1024.png` |

**Rules:**
- No transparency (alpha channel) — App Store rejects icons with alpha.
- No rounded corners in your asset — iOS clips them automatically.
- All files must be **PNG**.

**Quick steps:**
1. Design at 1024×1024 and scale down.
2. Drop all PNG files into `Assets.xcassets/AppIcon.appiconset/`.
3. Edit `Contents.json` in that folder to reference each filename (or use Xcode's icon editor which handles this automatically — drag images in).
4. Upload the 1024×1024 PNG to App Store Connect → App Information → App Icon.

---

## Recommended Workflow

1. Design the master icon at **1024×1024** in Figma / Sketch.
2. Export it as a flat PNG (no shadows — the OS adds them on iOS).
3. Use **[IconKitchen](https://icon.kitchen)** or **[AppIcon.co](https://appicon.co)** — paste your 1024px image and download all sizes for both platforms in one click.
4. Place the output files as described above.
