# Huezoo Android capture flows (the iOS ones are ios-*.yaml, left untouched)

Output: `app-store-assets/screenshots/android-phone/` (1080x2400) and `android-tablet/` (2560x1600 landscape).
PNGs are taken with `adb -s $S exec-out screencap -p > file.png` right after a flow ends; Maestro only drives the UI.
Debug package `xyz.ksharma.huezoo.debug`, release package `xyz.ksharma.huezoo` (they install side by side).

## Build

```sh
cd "$(git rev-parse --show-toplevel)"
./gradlew :androidApp:assembleDebug
# Release, signed with the machine's debug keystore through Gradle's injected signing properties (the repo keystore.jks is never read):
./gradlew :androidApp:assembleRelease \
  -Pandroid.injected.signing.store.file=$HOME/.android/debug.keystore \
  -Pandroid.injected.signing.store.password=android \
  -Pandroid.injected.signing.key.alias=androiddebugkey \
  -Pandroid.injected.signing.key.password=android
# -> androidApp/build/outputs/apk/{debug,release}/androidApp-{debug,release}.apk   (apksigner verify: v2 signed, not debuggable)
```

If a debug build fails with "... is located outside the root directory", the Gradle project cache holds stale
paths from an old checkout location; build once with `--project-cache-dir <empty dir>`.

## Emulator, demo mode, state

Same emulator, demo-mode and tablet-rotation commands as `Sumi/app-store-assets/listing/capture-flows/README.md`
(tablet landscape is `user_rotation 0` on this AVD). Also: for the Huezoo captures the emulator was put in system dark mode (`adb -s $S shell cmd uimode night yes`, reset with
`... night auto` at the end) (the app itself always uses its dark colour scheme, Theme.kt has no light scheme).
The "mobile" demo icon renders as a second wifi glyph (phone) or "3G" (tablet): on the tablet hide it with
`-e command network -e mobile hide`.

Fresh debug install, then once: `maestro --device $S test android-<class>-00-setup.yaml`
(dismisses Quick Note, Settings > SWITCH TO PAID, HIDE ADS, back to Home). Hide Ads lives in memory (DebugFlags),
so do not kill the app afterwards. Start the app with adb first if Maestro's launchApp hangs under host load:
`adb -s $S shell am start -n xyz.ksharma.huezoo.debug/xyz.ksharma.huezoo.MainActivity`.

## Screens

- 01-home: after setup. 03-color-memory: `android-<class>-03-color-memory.yaml`, capture as soon as the flow passes
  (wait until the decision phase "Same - or different?" shows, B live).
- 05-daily: `android-<class>-05-daily.yaml`. The date line says "November 24 - Same for everyone" (Daily is date-driven).
- 02-threshold: RELEASE build only (debug draws the TridentSpoke label and outlines the odd swatch). Network off so no ad
  request is made: `adb -s $S shell svc wifi disable; adb -s $S shell svc data disable`. Install the release APK
  (`adb -s $S install -r .../androidApp-release.apk`), launch `xyz.ksharma.huezoo`, GOT IT, ENTER SIMULATION, capture.
  Release is the free tier (Home shows "5 / 5 TRIES REMAINING"); re-enable network afterwards.
- 04-result: Color Memory reaches "MISSION OUTCOME: COMPLETE" (Threshold never does: it shows FLATLINED / FAILURE / LEGENDARY).
  Maestro cannot see chamber A's colour, so `android-cm_play.sh` (runs ON the device, `adb push` then `adb shell sh`) reads
  the screen and answers. Phone: `adb push android-cm_play.sh /data/local/tmp/cm.sh; adb shell sh /data/local/tmp/cm.sh 300 1500 800 1500 283 2239 797 2239 5 4`
  (pixel A, pixel B, SAME tap, DIFFERENT tap, rounds, first round to answer wrongly). Tablet used `android-cm_play.py` (host side).
  Start the player first, then tap the Color Memory card. The phone 04-result was actually produced by
  `android-phone-cm-calibration.sh` (answers SAME every round, result 2/5, COMPLETE); the answer logic in the player is not
  reliable on the phone (B is a different colour during the memorize phase there), so treat the player as a starting point.
