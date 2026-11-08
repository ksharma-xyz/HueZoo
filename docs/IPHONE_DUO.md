# iPhone Duo and resizable windows

Huezoo sizes its layout to the **window**, never to the device. iPhone Duo (closed and open),
iPad, landscape phones and resizable windows all go through the same code.

## Displays

| Display | Pixels | Points (3 px/pt) | Notes |
|---|---|---|---|
| Duo outer (closed) | 1398 x 2034 | 466 x 678 | Status bar and camera sit in a column on the **right** edge |
| Duo inner (open), portrait | 2007 x 2853 | 669 x 951 | |
| Duo inner (open), landscape | 2853 x 2007 | 951 x 669 | |
| Resize mode | arbitrary | about 375 to 1165 wide | |

Fold or hinge aware layout is out of scope. The app treats each display as a plain window.

## Toolchain

The app must be built with Xcode 27.1 or later (iOS 27.1 SDK), otherwise iOS letterboxes it on
Duo. Built with Xcode 26 the outer display shows a black strip where the status column is;
built with 27.x the app draws under it, which is why the inset rules below matter.
CI uses the `xcode-27` runner with an exact `27.1` pin (`build-ios.yml`,
`distribute-testflight.yml`). Do not set `UIRequiresFullScreen`; there is none today.

Apple requires Duo screenshots from April 2027 for apps built with the iOS 27.1 SDK or later.
Submitting them adds the optimized for Duo badge.

## Inset rules

All helpers live in `ui/theme/CornerSafeInsets.kt`.

- **Top**: keep `statusBarsPadding()` (or the status bar spacer in `HuezooTopBar`) and add
  `cornerClearanceTop()` after it. It is zero on phones and 16dp minus the status bar on the
  Duo outer display, where the top inset is about 0 and nothing else moves the title out of the
  rounded corner. Do not replace `statusBarsPadding()` with `windowInsetsPadding(safeDrawing)`
  on top: where a parent already consumed the status bars the gap doubles.
- **Sides**: `Modifier.safeHorizontalPadding()` is `safeDrawing.only(Horizontal)`. Compose on iOS
  reports `displayCutout` on the top edge only, so `displayCutout` padding is zero on the Duo
  outer display. Use `safeDrawing` for the sides.
- **Bottom**: `navigationBarsPadding()` as before (home indicator).
- **Backgrounds stay full-bleed**: apply the background before the padding.
  `AmbientGlowBackground` does this for you: glows are full-bleed, its content is inset.
  `HuezooTopBar` insets its button row; `HuezooBottomSheet` and `HuezooAlertDialog` inset their
  content. New screens built on `AmbientGlowBackground` need nothing extra for the sides.
- Pinned banner ads use `safeHorizontalPadding()` too.

## Window size rules

- `contentMaxWidth()` caps a screen's main column at `MaxContentWidth` (640dp) and fills
  narrower windows. Place it with `Alignment.TopCenter` so it centres on wide windows.
- Dialog cards are capped at 440dp.
- Color Memory switches on window **height**: below 560dp the HUD, headline, readout and answer
  buttons sit on the left and the chambers on the right (side by side, nothing scrolls away
  during the timed phases); above it they stack and the chambers flex to fill the space.
- Every other screen scrolls vertically, so short windows (phone held sideways) scroll rather
  than clip. Threshold and Daily add `BannerReserve` bottom padding so the swatches can scroll
  clear of the pinned banner.
- Swatch layouts are a fixed 300dp square, which fits every width from 375dp up.

## Testing on the Duo simulator

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'id=<duo-udid>' -derivedDataPath <scratch>/dd CODE_SIGNING_ALLOWED=NO build
xcrun simctl install <duo-udid> <scratch>/dd/Build/Products/Debug-iphonesimulator/Huezoo.app
xcrun simctl launch <duo-udid> xyz.ksharma.huezoo
xcrun simctl io <duo-udid> screenshot --display=1 outer.png   # outer, 1398x2034
xcrun simctl io <duo-udid> screenshot --display=3 inner.png   # inner, 2007x2853; black unless open
```

- Maestro drives the outer display (`maestro --device <udid> test flow.yaml`). Percent points
  work; the settings gear is exposed as the text `⚙`, the back button only by point.
- Settings has debug toggles (HIDE ADS, SWITCH TO PAID) that give clean screenshots and reach
  the leaderboard without a purchase. Debug builds only.
- To test landscape without rotating the simulator, pass
  `INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone=UIInterfaceOrientationLandscapeRight`
  (and `_iPad`) to `xcodebuild` for a throwaway build.
- A leftover ATT dialog from another app can sit over the screen: tap "Ask App Not to Track".

## Not covered

- Fold or hinge aware layout, and splitting content across the two inner halves.
- The inner display could not be lit from the command line; it is covered by wide-window
  layouts (iPad, landscape) rather than on the Duo itself.
