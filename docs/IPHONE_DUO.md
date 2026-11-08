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

Window classes come from the window **width** in dp (`windowClassFor`, read through
`BoxWithConstraints`), never from the device. Helpers live in `ui/theme/WindowClass.kt`.

| Class | Width | Layout |
|---|---|---|
| Compact | below 600dp | The phone layout, unchanged. Swatches are 300dp. |
| Medium | 600 to 840dp | One column capped at `MaxContentWidth` (640dp). Game swatches scale. |
| Expanded | 840dp and wider | Multi-pane layouts. |

- `contentMaxWidth()` caps a screen's main column at `MaxContentWidth` (640dp) and fills
  narrower windows. Place it with `Alignment.TopCenter` so it centres on wide windows.
- `PageColumn` is the page column for game screens: 640dp on Compact and Medium, and on Expanded
  `ExpandedMaxWidth` (1200dp) or the `expandedWidth` the screen passes. Threshold and Daily pass
  `GamePageWidth` (976dp = 360dp HUD pane + 24dp gap + 560dp swatch + 16dp insets), so the top
  bar's edges are the panes' outer edges with no leftover side space. Color Memory uses the full
  1200dp.
- Dialog cards are capped at 440dp.
- Panes are inset 16dp on the sides, the same inset as the top bar's buttons, and sit 24dp apart
  (`PaneGap`). Panes in a row start on the same line; the game panes also end on the same line.
- Secondary elements are sized from the primary one (swatch size, card unit) and never stretched
  to fill leftover space: extra space becomes margin.

### Home

- Compact and Medium: one scrolling column, as before.
- Expanded: top bar across the page, then two top-aligned panes. "You" (width 5/12 of the page,
  clamped 320 to 440dp): callsign, gems, streak, personal best, rank progress, leaderboard.
  "Play": the Threshold hero, then Daily and Color Memory side by side with equal heights, then
  "What is ΔE?". When the Play pane is narrower than 600dp (windows around 950dp) Daily and
  Color Memory stack instead so their text is not cut. The page scrolls when the window is short.

### Threshold and Daily

- `RadialSwatchLayout(size = ...)` scales the whole geometry (tiles, gap, container) from the
  300dp design size. Compact keeps 300dp.
- Medium, and Expanded portrait: stacked column, swatches scale to
  `min(width - 48dp, height - header, 520dp)` (never below 300dp), and the group is centred
  vertically.
- Expanded landscape (width greater than height, room for at least a `MinPaneSwatchSize`
  200dp swatch): a supporting pane. The left pane (360dp) has one left edge. Threshold: title
  with lives and tries directly under it at the top, the ΔE badge, caption and session best in the
  middle, feedback at the bottom. Daily: title and date, round chip with its dots, feedback. The
  swatch (up to 560dp, `min(pane width, window height - banner)`) sits in a row exactly as
  tall as the swatch, so the pane's top and bottom are the swatch's top and bottom.
- Phone held sideways (about 914x411dp) is Expanded by width: Threshold and Daily use this
  layout with a small swatch; Home scrolls its two columns.

### Color Memory

- Expanded (and tall enough): a fixed 360dp controls pane (HUD at the top, headline and readout,
  SAME/DIFFERENT pinned to the bottom) beside the two chambers. Chamber height is capped at 1.4
  times their width (it shrinks, never stretches), the row is centred vertically, and the
  controls pane is exactly as tall as the chambers so tops and bottoms coincide.
- Below 560dp window height, or when the Expanded row would be under 320dp tall (phone held
  sideways): HUD, headline, readout and buttons on the left, chambers on the right (nothing
  scrolls away during the timed phases).
- Otherwise they stack and the chambers flex to fill the space.

### Other screens

- Every other screen scrolls vertically, so short windows (phone held sideways) scroll rather
  than clip. Threshold and Daily add `BannerReserve` bottom padding so the swatches can scroll
  clear of the pinned banner.
- Settings, Leaderboard and the other reading screens keep a single 640dp column.

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
