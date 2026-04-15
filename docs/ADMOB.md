# Huezoo — AdMob Implementation Reference

Technical reference for the Google Mobile Ads SDK integration on iOS and Android.
For ad *placement rules* (which screens, free vs paid) see `MONETIZATION_DESIGN.md`.

---

## Key external resources

| Resource | URL / location |
|---|---|
| Google Mobile Ads SDK iOS download + release notes | https://developers.google.com/admob/ios/download |
| **Official iOS sample repo** (source of SKAdNetwork list + UMP consent pattern) | https://github.com/googleads/googleads-mobile-ios-examples |
| Google Mobile Ads SDK Android | https://developers.google.com/admob/android/quick-start |
| Official Android sample repo | https://github.com/googleads/googleads-mobile-android-examples |
| LexiLabs basic-ads (KMP wrapper used in this project) | https://github.com/LexiLabs-App/basic-ads |
| AdMob Console | https://admob.google.com |
| SKAdNetwork IDs reference (Google's list) | BannerExample/Info.plist in the iOS sample repo above |

---

## SDK versions in use

| Platform | SDK | How added |
|---|---|---|
| iOS | Google Mobile Ads SDK **13.x** (via `swift-package-manager-google-mobile-ads`) | SPM — `iosApp.xcodeproj` |
| Android | Google Mobile Ads (via `play-services-ads`) | Gradle dependency in `composeApp` |
| Shared (KMP) | `app.lexilabs.basic:basic-ads` | `libs.versions.toml` |

SPM package pin is in:
`iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved`

> **Note:** SDK 11+ renamed the Swift entry point from `GADMobileAds.sharedInstance()` to
> `MobileAds.shared`. Always check the release notes when upgrading — this was the reason
> ads silently failed to initialise in an earlier build of this project.

---

## iOS setup

### 1. Info.plist — required keys

```xml
<!-- Your AdMob App ID from AdMob Console → App Settings -->
<key>GADApplicationIdentifier</key>
<string>ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX</string>

<!-- Required for ATT dialog copy (iOS 14+) -->
<key>NSUserTrackingUsageDescription</key>
<string>This identifier is used to show you personalized ads and measure
ad performance. You can opt out at any time in your device Settings.</string>

<!-- SKAdNetwork entries — see section below -->
<key>SKAdNetworkItems</key>
<array>...</array>
```

### 2. SKAdNetworkItems

**This is the most common reason ads don't show on a new iOS app.**

Without the full list, ad networks cannot attribute installs and will not bid
on your inventory — fill rate is near zero even if the SDK initialises correctly.

The list must be sourced from the official Google sample repo (it is updated
with each SDK release):

```
https://github.com/googleads/googleads-mobile-ios-examples
→ Swift/admob/BannerExample/BannerExample/Info.plist
→ copy the entire <key>SKAdNetworkItems</key> block
```

Current entry count in this project: **50 entries** (SDK 13.x list, last updated 2026-04-07).
The live list is in `iosApp/iosApp/Info.plist`.

### 3. SDK initialisation — AppDelegate

The correct order is: **UMP consent → ATT → MobileAds.start()**.
`MobileAds.shared.start()` must only be called when `canRequestAds == true`.

```swift
import GoogleMobileAds
import AppTrackingTransparency
import UserMessagingPlatform

class AppDelegate: NSObject, UIApplicationDelegate {

    private var mobileAdsStarted = false

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: ...) -> Bool {
        FirebaseApp.configure()
        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        gatherConsentThenStartAds()
    }

    private func gatherConsentThenStartAds() {
        let params = UMPRequestParameters()
        params.tagForUnderAgeOfConsent = false
        UMPConsentInformation.sharedInstance.requestConsentInfoUpdate(with: params) { _ in
            let rootVC = /* resolve key window root VC */
            DispatchQueue.main.async {
                UMPConsentForm.loadAndPresentIfRequired(from: rootVC) { _ in
                    self.requestATT()
                    self.startMobileAdsIfAllowed()
                }
                self.startMobileAdsIfAllowed()  // no-op if canRequestAds == false
            }
        }
        startMobileAdsIfAllowed()  // prior-session consent already in place
    }

    private func startMobileAdsIfAllowed() {
        guard !mobileAdsStarted,
              UMPConsentInformation.sharedInstance.canRequestAds else { return }
        mobileAdsStarted = true
        MobileAds.shared.start(completionHandler: nil)
    }

    private func requestATT() {
        if #available(iOS 14, *) {
            ATTrackingManager.requestTrackingAuthorization { _ in }
        }
    }
}
```

**Why UMP before MobileAds.start()?**
Since 2024 Google requires a GDPR/CCPA consent form for EEA users via the
User Messaging Platform SDK. Without it AdMob will not serve ads to EEA users
and may suppress global fill. `canRequestAds` acts as the gate.

**Why `applicationDidBecomeActive` and not `didFinishLaunchingWithOptions`?**
Apple requires the ATT dialog to appear only after the app's UI is visible.
Requesting it during launch can cause the dialog to be suppressed or rejected
by Apple review. The UMP consent form has the same constraint.

### 4. ATT (App Tracking Transparency)

- Requires `NSUserTrackingUsageDescription` in Info.plist (already present)
- Without ATT permission, IDFA is all zeros → AdMob serves non-personalised
  ads only, with significantly lower fill rate and eCPM
- ATT is now requested **inside the UMP callback** so the two system dialogs
  never stack on top of each other
- The `AppTrackingTransparency` framework is built into iOS 14+ — no extra
  SPM dependency needed; just import it in Swift

### 5. UMP (User Messaging Platform / GDPR consent)

- SPM package: `swift-package-manager-google-user-messaging-platform` (already in Package.resolved)
- `UMPConsentInformation.sharedInstance.requestConsentInfoUpdate` must be
  called on every app foreground to refresh consent status
- `loadAndPresentIfRequired(from:)` is a no-op when consent is not required
  or already obtained — safe to call unconditionally
- `canRequestAds` is the single gate before `MobileAds.shared.start()`

---

## Android setup

### Manifest

```xml
<manifest>
    <application>
        <!-- Your AdMob App ID from AdMob Console → App Settings -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"/>
    </application>
</manifest>
```

### SDK initialisation

Called once in `Application.onCreate()` via `AdsInitializer.kt`:

```kotlin
// composeApp/src/androidMain/.../platform/ads/AdsInitializer.kt
fun initMobileAds(context: Context) {
    MobileAds.initialize(context)
}
```

No ATT equivalent on Android — the SDK handles consent via UMP (see below).

---

## KMP / Compose shared layer

### AdIds.kt

Central registry for ad unit IDs. Switches between Google's official test IDs
(debug builds) and live IDs (release builds) automatically.

```kotlin
AdIds.init(platformOps.isDebugBuild)  // called once in App()
```

Test IDs reference: https://developers.google.com/admob/android/test-ads

Live ad unit IDs are in AdMob Console → your app → Ad units.
Current IDs are stored in `composeApp/src/commonMain/.../platform/ads/AdIds.kt`.

### BasicAds.Initialize()

LexiLabs `BasicAds.Initialize()` is a Composable called at the top of `App()`.
It handles the KMP-level SDK wiring. The native `MobileAds.shared.start()` /
`MobileAds.initialize()` calls still need to happen at the native layer
(AppDelegate on iOS, Application on Android) *before* this composable runs.

### AdOrchestrator

Controls interstitial ad cadence for Threshold sessions:
- Max once per 2 sessions
- Never after a personal best
- Max 3 per calendar day

See `composeApp/src/commonMain/.../platform/ads/AdOrchestrator.kt`.

---

## Ad unit IDs

| Format | AdMob Console name | Used on screen |
|---|---|---|
| Banner | — | Home, Result, Threshold (playing), Daily |
| Interstitial | — | Threshold session end (via AdOrchestrator) |
| Rewarded | — | PaywallSheet "Watch Ad → +1 try" |

All ad placements are guarded by `!isPaid && !DebugFlags.hideAds`.
The Upgrade/Paywall screens never show ads (user is mid-purchase funnel).

---

## Testing

### iOS — test device setup

1. Run the app with a debug build (uses Google's official test ad unit IDs automatically via `AdIds.isDebug`)
2. For a release/TestFlight build add your device to AdMob Console:
   AdMob Console → Settings (gear icon) → **Test devices** → Add device
   Provide your device's IDFA (find it in Settings → Privacy → Tracking, or log it from `ASIdentifierManager`)
3. Test ads are always filled and clearly labelled "Test Ad" — no policy risk

### Android — test device setup

Same flow: AdMob Console → Settings → Test devices → add your device's advertising ID
(find it in Settings → Google → Ads → Your advertising ID).

### Debug flag

`DebugFlags.hideAds = true` (togglable in Settings screen, debug builds only)
hides all ad composables — useful for UI screenshots.

---

## Troubleshooting checklist

| Symptom | Likely cause | Fix |
|---|---|---|
| **Ads show in debug, not production (iOS)** | UMP consent form never presented → `canRequestAds` is false for EEA users → `MobileAds.start()` never called | Implement `UMPConsentForm.loadAndPresentIfRequired` before `MobileAds.shared.start()` — see iOSApp.swift |
| No ads on iOS new install | SKAdNetworkItems incomplete | Copy full list from Google sample repo BannerExample/Info.plist |
| No ads after updating SDK | `GADMobileAds` renamed | Use `MobileAds.shared.start()` (SDK 11+) |
| No ads, ATT not prompted | Missing ATT request | `requestATT()` is now called inside the UMP consent callback in `iOSApp.swift` |
| Ads show in debug, not release | `isDebugBuild` wrong or AdMob app not verified | Check `Platform.isDebugBinary` return value; check AdMob Console for verification banner |
| Very low fill rate on new app | AdMob account/app not yet verified | Wait 24–48h after first traffic; check AdMob Console for policy issues |
| `GADMobileAds` not found | Wrong SDK version | Use `MobileAds.shared` — renamed in SDK 11 |
| Rewarded ad crashes | Ad not loaded before show | Check `AdState.READY` before calling `InterstitialAd` / `RewardedAd` composable |
