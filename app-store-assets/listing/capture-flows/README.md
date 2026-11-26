# Huezoo iOS store screenshots: capture flows

Raw App Store screenshots of the iOS app, driven by Maestro and captured with `simctl`.
Android is captured separately (see `android-README.md`).

| File | Screen | Build | Flow |
|---|---|---|---|
| `01-home.png` | Home, top of scroll | Debug | none (state left by `ios-setup.yaml`) |
| `02-threshold.png` | The Threshold, round 1, before any tap | **Release** | `ios-threshold-round.yaml` |
| `03-color-memory.png` | Color Memory, "Same — or different?" | Debug | `ios-color-memory-decision.yaml` |
| `04-result.png` | Result screen, "MISSION OUTCOME: COMPLETE" | Debug | `ios-result.yaml` via `result-retry.sh` |
| `05-daily.png` | Daily Challenge, round 1 | Debug | `ios-daily.yaml` |

Output folders (under `app-store-assets/screenshots/`):

| Class | Simulator | Folder | Pixels |
|---|---|---|---|
| iPhone Duo outer | `iphone-duo` | `iphone-duo-outer-1398x2034` | 1398 x 2034 (`DISPLAY_ID=1`) |
| iPhone 6.9" | `store-69` (iPhone 17 Pro Max) | `iphone-6.9in-1320x2868` | 1320 x 2868 |
| iPad 13" | `store-ipad13` (iPad Pro 13-inch M5) | `ipad-13in-2064x2752` | 2064 x 2752 |

## Why two builds

The Debug build draws a layout-name label ("TridentSpoke", "YSpoke") above the swatches and
outlines the odd swatch, so the Threshold frame (02) comes from the Release build. Release has
no debug toggles; it shows no ad because **paid status persists** in the app database across the
reinstall (only HIDE ADS is in memory and is lost on every relaunch).

## Build once (about 10 minutes cold)

```sh
cd "$(git rev-parse --show-toplevel)"
S=<scratch>/huezoo-ios
UDID=<any simulator udid>
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination "id=$UDID" -derivedDataPath $S/dd-Debug CODE_SIGNING_ALLOWED=NO build
# Release needs ONLY_ACTIVE_ARCH + ARCHS, otherwise Xcode builds arm64+x86_64 and the Kotlin
# step fails with "Cannot query the value of this provider because it has no value available".
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release \
  -destination "id=$UDID" -derivedDataPath $S/dd-Release CODE_SIGNING_ALLOWED=NO \
  ONLY_ACTIVE_ARCH=YES ARCHS=arm64 build
DEBUG_APP=$S/dd-Debug/Build/Products/Debug-iphonesimulator/Huezoo.app
RELEASE_APP=$S/dd-Release/Build/Products/Release-iphonesimulator/Huezoo.app
```

## Rules that matter

- **Target one simulator by UDID.** Other running simulators are left alone, since someone else may
  be using them. Maestro is pinned with `--device <udid>`, pixels come from
  `xcrun simctl io <udid> screenshot` (never from Maestro).
- **Never relaunch the Debug app between captures.** HIDE ADS lives in `DebugFlags` in memory.
  After a relaunch or a reinstall over Release, run `ios-hide-ads.yaml` again.
- **Back button is by point only.** Pass it per class as `-e BACK_POINT=<x%>,<y%>` (integer
  percents). Mid-game back asks "LEAVE SESSION?"; `ios-back-home.yaml` confirms it.
- **The Duo camera circle flickers into some frames** (a black disc at about 1255,140 px).
  `capture-ios.sh` retakes the frame until it is gone when `DISPLAY_ID=1` is set.
- Size is verified and never resized: a wrong size aborts the capture.

## Order per class (this is why 02 comes before 04)

04 earns gems, which would show in the "N GEMS" badge on the Threshold screen. So 02 is captured
at 0 gems, before 04, to match 01 (0 gems):

```sh
F="$(git rev-parse --show-toplevel)/app-store-assets/listing/capture-flows"
OUT="$(git rev-parse --show-toplevel)/app-store-assets/screenshots/<folder>"
U=<udid>; BACK=<x%>,<y%>; W=<width>; H=<height>
# (Duo only: export DISPLAY_ID=1 and add nothing else.)

# 0. boot the target simulator, status bar override (iPhone and iPad; works on Duo too)
xcrun simctl boot $U; xcrun simctl bootstatus $U -b
xcrun simctl status_bar $U override --time 9:41 --batteryState charged --batteryLevel 100 \
  --wifiBars 3 --cellularBars 4

# 1. fresh Debug install, launch, canonical state (first-run notice, PAID, HIDE ADS)
xcrun simctl uninstall $U xyz.ksharma.huezoo
xcrun simctl install $U $DEBUG_APP && xcrun simctl launch $U xyz.ksharma.huezoo; sleep 5
(cd $F && maestro --device $U test -e BACK_POINT=$BACK ios-setup.yaml)

# 2. 01, 03, 05 from the Debug build
$F/capture-ios.sh $U $OUT/01-home.png $W $H -
$F/capture-ios.sh $U $OUT/03-color-memory.png $W $H ios-color-memory-decision.yaml
(cd $F && maestro --device $U test -e BACK_POINT=$BACK ios-back-home.yaml)
$F/capture-ios.sh $U $OUT/05-daily.png $W $H ios-daily.yaml
(cd $F && maestro --device $U test -e BACK_POINT=$BACK ios-back-home.yaml)

# 3. 02 from the Release build (installed over Debug, so PAID persists, no debug label)
xcrun simctl terminate $U xyz.ksharma.huezoo
xcrun simctl install $U $RELEASE_APP && xcrun simctl launch $U xyz.ksharma.huezoo; sleep 8
$F/capture-ios.sh $U $OUT/02-threshold.png $W $H ios-threshold-round.yaml

# 4. 04 from the Debug build again (HIDE ADS was lost, so re-apply it)
xcrun simctl terminate $U xyz.ksharma.huezoo
xcrun simctl install $U $DEBUG_APP && xcrun simctl launch $U xyz.ksharma.huezoo; sleep 5
(cd $F && maestro --device $U test -e BACK_POINT=$BACK ios-hide-ads.yaml)
$F/result-retry.sh $U $OUT/04-result.png $W $H $BACK 10

# 5. verify
sips -g pixelWidth -g pixelHeight $OUT/*.png
```

## Per class values

| Class | Simulator UDID (recreate if gone) | `W x H` | `BACK_POINT` | Extra `ios-back-home.yaml` points |
|---|---|---|---|---|
| Duo outer | `<UDID>` (an iPhone Duo simulator) | 1398 x 2034, `DISPLAY_ID=1` | `11%,7%` | none |
| iPhone 6.9" | `xcrun simctl create store-69 com.apple.CoreSimulator.SimDeviceType.iPhone-17-Pro-Max com.apple.CoreSimulator.SimRuntime.iOS-27-2` | 1320 x 2868 | `12%,10%` | none |
| iPad 13" | `xcrun simctl create store-ipad13 com.apple.CoreSimulator.SimDeviceType.iPad-Pro-13-inch-M5-16GB com.apple.CoreSimulator.SimRuntime.iOS-27-2` | 2064 x 2752 | `24%,5%` (Settings and Result) | `-e BACK_POINT_2=5%,5%` (Color Memory) and `-e BACK_POINT_3=8%,5%` (Threshold, Daily) |

On iPad the top bar sits at a different x on each screen because the page column width differs,
so the back button is not at one point. `ios-back-home.yaml` taps all three; the top-left strip
holds nothing else. Pass them on every `ios-back-home.yaml` and `result-retry.sh` run on iPad.
`result-retry.sh` only takes `BACK_POINT`; on iPad the Result screen back button is at `24%,5%`,
so that is the one to pass.

### iPad: switch the simulator to Full-Screen Apps first

A fresh iPad simulator runs "Windowed Apps": it draws a window resize grip (a small arc) in the
bottom-right corner and adds the app name to the status bar. Turn it off once per iPad simulator:

```sh
xcrun simctl launch $U com.apple.Preferences
(cd $F && maestro --device $U test <(printf 'appId: com.apple.Preferences\n---\n- tapOn: "Multitasking & Gestures"\n- tapOn: "Full-Screen Apps"\n'))
# then terminate Settings, reinstall and cold-launch Huezoo, otherwise the status bar shows a
# "< Settings" back link
xcrun simctl terminate $U com.apple.Preferences; xcrun simctl terminate $U xyz.ksharma.huezoo
```

### Status bar override

`xcrun simctl status_bar $U override --time 9:41 --batteryState charged --batteryLevel 100
--wifiBars 3 --cellularBars 4`: Duo, iPhone and iPad all applied it (9:41, full charged battery,
full signal). On iPad the status bar also shows the real date ("Tue 24 Nov"); `simctl` cannot
override it.

## The result screen (04)

The Threshold result banner is "MISSION OUTCOME: FAILURE" (or FLATLINED), never COMPLETE. The
"COMPLETE" screen (gems earned, new personal best, "CORRECT ROUNDS 2 / 5") is the **Color Memory**
result, so 04 is a Color Memory session: five SAME/DIFFERENT
answers. Whether a pair is the same is a coin flip in the engine and cannot be read from the UI,
so `result-retry.sh` repeats the session with random answers until the banner is COMPLETE
(LEGENDARY adds a rainbow frame, FLATLINED means a score of 0 or below) with "NEW PERSONAL BEST".
Scores and gems therefore differ per class.

## Flow files

| File | Purpose |
|---|---|
| `ios-setup.yaml` | First run: dismiss the eye-strain notice, Settings, SWITCH TO PAID, HIDE ADS, back to Home |
| `ios-hide-ads.yaml` | Re-apply HIDE ADS only (after a relaunch or reinstall) |
| `ios-hide-ads-from-settings.yaml` | Sub-flow used by both |
| `ios-back-home.yaml` | Back to Home from anywhere, confirming "LEAVE SESSION?" |
| `ios-threshold-round.yaml` | Home to Threshold round 1 |
| `ios-color-memory-decision.yaml` | Home to Color Memory decision phase |
| `ios-cm-round.yaml` | One Color Memory answer |
| `ios-result.yaml` | Color Memory, 5 answers, assert COMPLETE + new personal best |
| `ios-daily.yaml` | Home to Daily Challenge round 1 |
| `capture-ios.sh` | Boot the target sim, run a flow, simctl screenshot, verify size |
| `result-retry.sh` | Repeat `ios-result.yaml` with random answers until it is COMPLETE |
| `pixel.py` | Reads one pixel from a BMP (Duo camera-circle check) |

## Gotchas found

- Maestro text selectors ignore case, so `HIDE ADS` also matches the row label "Hide Ads"; the
  selector is anchored `below:` the row description.
- `tapOn` fails on an element scrolled out of view; use `scrollUntilVisible` first.
- `takeScreenshot` only writes inside Maestro's own output folder; use `simctl io` instead.
- Percent points must be integers (`6.8%` throws `NumberFormatException`).
- Home is one scrolling column (the header scrolls away), so "Home reached" is any of several
  Home-only texts, not just `CURRENT INVENTORY`.
