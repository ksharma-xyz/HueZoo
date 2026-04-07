# Huezoo — MVP

*One obsessive game loop. Ship it. Hook people. Monetize the obsession.*

Completed phases archived in `docs/archive/MVP_COMPLETED.md`.

---

## Progress

| Phase | What | Status |
|---|---|---|
| 0 | Project setup, DI, nav, SQLDelight | ✅ |
| DS.0–DS.2 | Design system, fonts, components | ✅ |
| DS.3 | Haptics | ✅ |
| DS.4 | Sound | ⬜ |
| DS.5 | Animations | ✅ |
| 1–6 | Color math, game loops, screens | ✅ |
| UX | Various UX polish (see below) | Partial |
| 7 | Monetization — AdMob + IAP | ✅ |
| 8 | Firebase leaderboard + auth | ⬜ |
| 9 | Polish + ship | ⬜ |
| CI | CI/CD pipeline — workflows created, **one-time setup required** | ⬜ |
| T | Unit tests | Partial |
| L | Font license review | ✅ |

---

## Core Idea

**The Threshold** — detect the smallest color difference you can. 5 tries per 8h window (free) / unlimited (paid). Best ΔE is your result.
**Daily Challenge** — date-seeded, same for every player, 6 rounds, 1 attempt per day.

```
Open app
  ├── Daily Challenge  →  1 attempt/day  →  Result  →  Share
  └── The Threshold   →  5 tries / 8 hours
                            ├── Out of tries → Watch Ad (+1) OR Unlock $2
                            └── Result → Share
```

---

## Monetization

| Feature | Free | Paid ($2 one-time) |
|---|---|---|
| Daily Challenge | ✅ 1/day | ✅ 1/day |
| Threshold attempts | 5 per 8h | ♾️ Unlimited |
| Watch ad for +1 try | ✅ | Not needed |
| Leaderboard | ❌ | ✅ |
| Ads | shown | removed |

---

## Pending Work

### DS.4 — Sound ⬜
- [ ] DS.4.1 Source + add sound files: `correct`, `wrong`, `gameover`, `tick`, `gem_earned`, `button_tap`
- [ ] DS.4.2 `SoundType` enum + `SoundEngine` interface in commonMain
- [ ] DS.4.3 Android actual — `SoundPool`; iOS actual — `AVAudioPlayer`
- [ ] DS.4.4 `isSoundEnabled` setting in `SettingsRepository` (SQLDelight `user_settings`), default on
- [ ] DS.4.5 Sound on/off toggle button (speaker icon) — in `HuezooTopBar` or persistent on game screens
- [ ] DS.4.6 Wire into gameplay: correct → `correct`, wrong → `wrong`, game over → `gameover`, gem → `gem_earned`, button → `button_tap`

### DS.Font — Pre-ship items ⬜
- [ ] DS.Font.6 Verify previews render with correct fonts in Android Studio
- [ ] DS.Font.7 Verify on-device on Android + iOS simulator
- [ ] DS.Font.8 License review (see Phase L)

### UX.5 — Onboarding ~~⬜~~ (deferred — not in v1)
- ~~UX.5.1 First-launch onboarding~~ — deferred
- ~~UX.5.2 Review subtitle copy~~ — deferred
- ~~UX.5.3 Eye strain / health notice~~ — deferred

### UX.6 — Navigation Gaps ✅
- [x] UX.6.2 Leaderboard button: shows graceful "Signal Offline" placeholder for paid users — not broken, acceptable for v1

### UX.7 — Directional Feedback ⬜
- [ ] UX.7.2 ΔE tier label in HUD (below ΔE chip): "BEGINNER / TRAINING / SHARP / EXPERT / ELITE"
- [ ] UX.7.3 Tier-change animation: brief pulse when ΔE crosses a tier boundary
- [ ] UX.7.4 First-round tooltip: "Lower ΔE = harder to spot" — one-time, dismisses on first tap
- [ ] UX.7.5 Daily: per-round base color variety — each round seeds a different base color

### UX.8 — Correct Swatch Dismiss ⬜
- [ ] UX.8.1 Dismiss animation for the correct swatch (pop / glow-burst / implode)

### UX.9 — Gem Float-Up ⬜
- [ ] UX.9.3 In-game "+N 💎" float-up label from HUD on earn (fades 600ms)

### UX.11 — Out of Tries Refill Sheet ✅
- [x] UX.11.1 Refill bottom sheet — gem refill + Watch Ad options; replaces current full-screen Blocked state
- [x] UX.11.2 Decide gem refill cost + try count (draft in GAME_DESIGN.md first)
- [x] UX.11.3 Gem deduction via `SettingsRepository.addGems(-N)`; disable if insufficient
- [x] UX.11.4 Watch Ad button stub (no-op until AdMob Phase 7)

### UX.12 — In-Game Streak ⬜
- [ ] UX.12.1 Track consecutive correct taps in `ThresholdViewModel`
- [ ] UX.12.2 5-in-a-row: confetti burst + "5 STREAK!" banner (800ms)
- [ ] UX.12.3 10-in-a-row: multi-color confetti + "UNSTOPPABLE!" + bonus gems
- [ ] UX.12.4 Streak counter in HUD (appears after first correct tap, gone on wrong)

### UX.13 — Result Polish ⬜
- [ ] UX.13.1 "NEW PERSONAL BEST" badge on ΔE stat card
- [ ] UX.13.2 ΔE tier name on result (e.g. "EXPERT" badge alongside ΔE value)
- [ ] UX.13.3 Neon border redesign — Result screen gets a neon rectangular border
      inspired by the `docs/ideas/screen.png` promo art (cyan/magenta/yellow glow edges).
      Replaces or augments the current card background. Subtle animated glow on entry.
      Design ref: `docs/ideas/screen.png` outer rectangle border.

### UX.14 — Swatch Size Setting ⬜
- [ ] UX.14.1 Wire `SwatchSize` to `user_settings` in SQLDelight
- [ ] UX.14.2 Toggle in Settings screen

### UX.15 — Settings / About Screen ~~⬜~~ (deferred — not in v1)
- ~~UX.15.1 About screen~~ — deferred
- ~~UX.15.2 Health & eye strain notice~~ — deferred
- ~~UX.15.3 Privacy Policy link~~ — deferred

### UX.20 — Share from PerceptionTiersSheet ⬜
- [ ] UX.20.1 Full-width share button at bottom of `PerceptionTiersSheet`
- [ ] UX.20.2 Share text: `"My best ΔE is {deltaE} — {tierLabel}. Can you beat it? huezoo.app"`
- [ ] UX.20.3 Unranked copy variant
- [ ] UX.20.4 Platform share icon (iOS / Android)

### UX.21.5 — ShelfPress Modifier ⬜
- [ ] UX.21.5 `Modifier.shelfPress(shelfHeight, shelfColor, shape)` reusable extension

### UX.22 — Copy & UX Debt ⬜
- [x] UX.22.1 Audit and replace arrow "→" copy across entire app — feels AI-generated, replace with better phrasing
- [ ] UX.22.2 Help / settings top-bar buttons: add same 3D shelf-press animation as back button

### UX.23 — Debug Log Cleanup ✅
- [x] UX.23.1 Remove all `[DEBUG_PAYWALL]`, `[DEBUG_DELTA]`, `[DEBUG_RESULT]` println logs from `PaywallViewModel`, `PaywallSheet`, `ThresholdViewModel`, `ResultViewModel`

### Phase 7 — Monetization ✅
- [x] 7.1 Attempt counter on Threshold card ("X of 5 tries used this window")
- [x] 7.2 Out of Tries refill sheet (UX.11) — `PaywallSheet` with gem spend + watch ad + unlock forever
- [x] 7.3a Wire real IAP in `UpgradeScreen.onPurchase` — `UpgradeViewModel` + `AndroidBillingClient`
- [x] 7.3b Fetch price string from store at runtime — `billingClient.queryPrice()` in `UpgradeViewModel`
- [x] 7.3c Result screen entry point for upgrade CTA (after out-of-tries)
- [x] 7.4 AdMob — rewarded ad for +1 try (Android; iOS stub — StoreKit deferred)
- [x] 7.5 IAP — one-time "Unlimited" product (Google Play Billing; StoreKit 2 deferred)
- [x] 7.6 Persist `is_paid = true` in SQLDelight on purchase
- [x] 7.7 Paid: remove attempt cap, show "∞ UNLIMITED" badge on Threshold card
- [x] 7.8 Leaderboard gating — free tier taps → `UpgradeScreen`

### Phase 8 — Firebase Leaderboard ⬜
*Paid-only feature. All items behind `isPaid` flag.*
- [ ] 8.0 Gate leaderboard navigation behind `isPaid`
- [ ] 8.1 Firebase project — enable Realtime DB + Anonymous Auth
- [ ] 8.2 Firebase config files (Android + iOS)
- [ ] 8.3 Add `firebase-gitlive` KMP SDK to `libs.versions.toml`
- [ ] 8.4 Schema: `/leaderboard/{uid}: { name, deltaE, timestamp }`
- [ ] 8.5 Query: order by `deltaE` asc, limit 50
- [ ] 8.6 `LeaderboardScreen` — ranked list, your entry highlighted
- [ ] 8.7 Submit flow: name input sheet → push to Firebase
- [ ] 8.8 Security rules: public read, anon-auth write, max 1 entry per UID
- [ ] 8.9 Player rank: query position after submission; wire into `HomeUiState.Ready.rank`

### Phase 9 — Polish & Ship ⬜
- [x] 9.0 UI buttons use drawn shapes in production — `BackChevron` (Canvas), help `"?"` text, settings `"⚙"` text; white-circle placeholder only in `@PreviewComponent` private previews, not in shipped UI
- [ ] 9.1 App icon final artwork — iOS: replace Light/Dark/Tinted PNGs in `AppIcon.appiconset` (1024×1024 each); Android: update `android_logo.xml` + `ic_launcher_background.xml` vector drawables. Single-size/adaptive approach already wired correctly on both platforms.
- [x] 9.2 Tested on real Android + iOS device
- [ ] 9.3 Haptic + sound tuning pass on real device
- [ ] 9.4 Play Store listing — screenshots, description, content rating
- [ ] 9.5 App Store listing — screenshots, description, review submission

### Phase T — Testing ⬜
- [ ] T.1 `ColorMath.deltaE()` — round-trip tests vs CIEDE2000 reference values
- [ ] T.2 `PlayerLevel.fromGems()` — all tier boundaries
- [ ] T.3 `GameRewardRates` — verify gem rate constants
- [ ] T.4 `DefaultThresholdGameEngine` — ΔE progression, floor, personal best logic
- [ ] T.5 `DefaultDailyGameEngine` — correct/wrong round handling, participation + perfect gem, 6-round-always rule
- [ ] T.6 `ThresholdViewModel` — drive via `onUiEvent()`, verify gem accumulation, milestone bonuses, try budget exhaustion
- [ ] T.7 `DailyViewModel` — per-round flow, perfect bonus, ΔE-in-result = highest correct round ΔE
- [x] T.8 Attempt window — 8h window logic, free vs paid caps, expiry reset (`ThresholdAttemptWindowTest`)
- [x] T.8.1 Bonus try availability — `bonusTries > 0` when base exhausted → Available (regression for double-shrink formula)
- [x] T.8.2 Earn→play→earn cycle — stays Available after second ad reward (exact regression case)
- [x] T.8.3 Visual heart cap — bonus tries > maxAttempts are capped at maxAttempts hearts
- [x] T.8.4 All bonus tries exhausted → Exhausted

### Phase L — Font Licenses ✅
- [x] L.1 Clash Display (Fontshare FF EULA) — confirmed free for commercial use including APK/IPA bundling
- [x] L.2 Bebas Neue (SIL OFL 1.1) — no action needed
- [x] L.3 Space Grotesk (SIL OFL 1.1) — no action needed
- [ ] L.4 Remove unused fonts (Antonio, Fredoka) from `composeResources/font/`
- [x] L.5 Font attribution not required by Clash Display EULA

### Phase CI — CI/CD Pipeline ⬜ (workflows created, one-time setup required)

Workflows are in `.github/workflows/`. See `docs/ci_cd/CI_SETUP_CHECKLIST.md` for the full setup checklist.

**GitHub Secrets** (Settings → Secrets and variables → Actions → Secrets):
- [ ] CI.1 `PAT_HUEZOO_GITHUB` — Personal Access Token (`repo` scope + Variables read/write)
- [ ] CI.2 `ANDROID_KEYSTORE_FILE` — base64-encoded `.jks` signing keystore
- [ ] CI.3 `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`
- [ ] CI.4 `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` — Google Play Console service account key
- [ ] CI.5 `APPSTORE_KEY_ID`, `APPSTORE_ISSUER_ID`, `APPSTORE_PRIVATE_KEY` — App Store Connect API key
- [ ] CI.6 `IOS_DIST_SIGNING_KEY_BASE64`, `IOS_DIST_SIGNING_KEY_PASSWORD` — Apple Distribution certificate
- [ ] CI.7 `IOS_PROVISIONING_PROFILE_NAME` — App Store provisioning profile name

**GitHub Variables** (Settings → Secrets and variables → Actions → Variables):
- [ ] CI.8 `ANDROID_VERSION_CODE` = `1`
- [ ] CI.9 `IOS_BUILD_NUMBER` = `1`
- [ ] CI.10 `DEVELOPMENT_TEAM` = Apple Team ID (developer.apple.com → Membership)

**One-time iOS Xcode setup** (required before first TestFlight upload):
- [ ] CI.11 Set Bundle Identifier to `xyz.ksharma.huezoo` in Xcode → iosApp target → General
- [ ] CI.12 Add `CFBundleShortVersionString` (`1.0.0`) and `CFBundleVersion` (`1`) to `iosApp/iosApp/Info.plist`
- [ ] CI.13 Set `CURRENT_PROJECT_VERSION = 1` in Xcode → iosApp target → Build Settings
- [ ] CI.14 Register `xyz.ksharma.huezoo` in App Store Connect

**Firebase** (configure after Phase 8):
- [ ] CI.15 Uncomment Firebase workflow steps in `build-android.yml`, `build-ios.yml`, `distribute-testflight.yml`
- [ ] CI.16 Add `FIREBASE_SERVICE_ACCOUNT_KEY`, `FIREBASE_ANDROID_DEBUG_APP_ID`, `FIREBASE_ANDROID_PROD_APP_ID`, `FIREBASE_GOOGLE_SERVICES_JSON_DEBUG`, `FIREBASE_GOOGLE_SERVICES_JSON_RELEASE`, `FIREBASE_IOS_GOOGLE_INFO` secrets

### Pre-Launch — Android ⬜
- [x] PL.A.1 AdMob App IDs confirmed production — Android `~1632380205` in `androidApp/src/main/AndroidManifest.xml`, iOS `~2990794956` in `Info.plist`; ad unit IDs switch test↔prod by build type in `AdIds.kt`
- [ ] PL.A.2 Signing keystore created; `ANDROID_KEYSTORE_FILE` + password secrets added (see CI.2–CI.4)
- [ ] PL.A.3 `versionCode` and `versionName` set in `build.gradle.kts` (or driven by `ANDROID_VERSION_CODE` GitHub Variable)
- [ ] PL.A.4 ProGuard / R8 rules verified for release build — check SQLDelight, Koin, AdMob, Billing
- [ ] PL.A.5 Content rating questionnaire completed in Play Console
- [ ] PL.A.6 GDPR consent form configured in AdMob dashboard (required for EU users)
- [ ] PL.A.7 Privacy Policy URL added to Play Store listing (https://ksharma-xyz.github.io/HueZoo/privacy-policy/) — no in-app link needed
- [ ] PL.A.8 Tested on at least one real Android device (release build)

### Pre-Launch — iOS ⬜
- [ ] PL.I.1 Bundle Identifier set to `xyz.ksharma.huezoo` in Xcode → iosApp target → General (see CI.11)
- [ ] PL.I.2 `CFBundleShortVersionString` (`1.0.0`) and `CFBundleVersion` (`1`) in `iosApp/iosApp/Info.plist` (see CI.12)
- [x] PL.I.3 `NSUserTrackingUsageDescription` already in `Info.plist`; ATT prompt wired in `iOSApp.swift`
- [ ] PL.I.4 App Store Connect record created for `xyz.ksharma.huezoo` (see CI.14)
- [ ] PL.I.5 Apple Distribution certificate + App Store provisioning profile created; secrets added (CI.6–CI.7)
- [ ] PL.I.6 StoreKit 2 IAP product configured in App Store Connect (currently stub — deferred from Phase 7)
- [ ] PL.I.7 TestFlight build uploaded and internal testing completed
- [ ] PL.I.8 Privacy Policy URL added to App Store Connect listing (https://ksharma-xyz.github.io/HueZoo/privacy-policy/)
- [ ] PL.I.9 Tested on at least one real iOS device (release build)

### Pre-Launch — Common ⬜
- [x] PL.C.1 Privacy Policy live at https://ksharma-xyz.github.io/HueZoo/privacy-policy/ — paste URL into Play Console + App Store Connect store listing metadata (no in-app link needed)
- [ ] PL.C.2 App icon all sizes generated for Android (mipmap) and iOS (Assets.xcassets) — see 9.1
- [ ] PL.C.3 Font license review complete — Clash Display EULA permits APK/IPA bundling (see Phase L)
- [ ] PL.C.4 All GitHub CI/CD secrets and variables configured (see Phase CI)

---

## Ship Checklist

- [x] Phase 7 (monetization) complete
- [x] UX.23 debug logs removed
- [ ] Phase 8 live or behind feature flag (deferred — leaderboard shows placeholder)
- [ ] DS.4 sound wired (deferred for v1)
- [x] Phase L font license review
- ~~UX.5.1 first-launch onboarding~~ — deferred
- [x] UX.6.2 leaderboard placeholder acceptable for v1
- [ ] App icon final artwork (9.1)
- [x] Tested on real Android + iOS device (9.2)
- [x] PL.A.1 AdMob IDs production ✅
- [x] PL.I.3 NSUserTrackingUsageDescription ✅
- [ ] Pre-Launch Android checklist (PL.A.2–PL.A.8)
- [ ] Pre-Launch iOS checklist (PL.I.1–PL.I.2, PL.I.4–PL.I.9)
- [ ] Pre-Launch Common checklist (PL.C.1–PL.C.4)
