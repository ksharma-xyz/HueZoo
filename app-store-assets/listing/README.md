# Huezoo store panels

Premium panels taken from Huezoo's own theme: the #0D0D16 canvas, corner glows in each game's colour, one six-petal flower with one petal off, the HUEZ wordmark, Clash Display headline and Space Grotesk subline;
upright device with a slim frame. Keep it restrained: one hero artwork. Rejected and old drafts (`app-store-assets/_*/`) stay local.

Pictures in `app-store-assets/` are stored with Git LFS. If they show up as small text files, run
`git lfs pull --include="app-store-assets/**"`.

## Rebuild

```sh
cd app-store-assets/listing
python3 render.py listing.json                  # every class whose raw captures all exist
python3 render.py listing.json --class ipad-13  # one class
python3 render.py listing.json --verify         # exact pixels, RGB, 1-10 files, copy rules
```

Needs Google Chrome (headless) and Pillow. A class with any raw file missing is skipped, loudly.

## Inputs and outputs

- Raw native captures (never resized): `../screenshots/<class dir>/<NN-name>.png`, mapped per class in `listing.json`.
- Copy, colours and panel order: `listing.json`. Each panel has `lines` (two headline lines, at most 7
  words, no full stop), `sub`, optional `seal` and `colors`.
- Background artwork: `art.js` (canvas, seeded from the panel slug so rebuilds are identical).
  Type and ornaments: `art.css`. `render.py` is the same file in Sumi and Huezoo.
- Output: `store-images/<class>/<NN>_<slug>.png` at the exact store size.

## Classes

| Class | Pixels | Store slot |
|---|---|---|
| iphone-6.9 | 1320 x 2868 | App Store, iPhone 6.9" (required) |
| iphone-6.3 | 1206 x 2622 | App Store, iPhone 6.1" or 6.3" (optional, rendered from the 6.9" captures) |
| iphone-6.5 | 1242 x 2688 | App Store, iPhone 6.5" (optional, scaled from 6.9") |
| ipad-13 | 2064 x 2752 | App Store, iPad 13" |
| iphone-duo-outer | 1398 x 2034 | App Store, iPhone Duo outer (from April 2027) |
| iphone-duo-inner-landscape | 2853 x 2007 | App Store, iPhone Duo inner |
| iphone-duo-inner-portrait | 2007 x 2853 | App Store, iPhone Duo inner |
| android-phone | 1080 x 1920 | Google Play, phone |
| android-tablet | 1920 x 1080 | Google Play, 10-inch tablet |

## iPhone Duo inner (still to capture)

Unfold in Xcode's DeviceHub.app: the posture buttons under the device (AppleScript `click at` works on
them, but not on the app itself). Capture with `xcrun simctl io <udid> screenshot --display=3`
(2853 x 2007). Maestro does not work while unfolded. Never use `screenConfig power off`.
Save to `../screenshots/iphone-duo-inner-2853x2007/` with the usual filenames, then rerun render.py.
02-threshold needs a Release build.
