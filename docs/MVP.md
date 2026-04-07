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
| DS.4 | Sound | ⬜ deferred — not in v1 |
| DS.5 | Animations | ✅ |
| 1–6 | Color math, game loops, screens | ✅ |
| UX | Various UX polish (see below) | Partial |
| 7 | Monetization — AdMob + IAP | ✅ |
| 8 | Firebase leaderboard + auth | ⬜ deferred — placeholder shown |
| 9 | Polish + ship | ✅ |
| CI | CI/CD pipeline — workflows created, **one-time setup required** | ⬜ |
| T | Unit tests | ✅ |
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

### UX.7 — Directional Feedback (partial)
- [x] UX.7.2 ΔE tier label in HUD (below ΔE chip): live `estimatedPerceptionTier(deltaE).rankLabel`
- ~~UX.7.3~~ Tier-change animation — deferred
- ~~UX.7.4~~ First-round tooltip — deferred
- [ ] UX.7.5 Daily: per-round base color variety — each round seeds a different base color

### UX.6 — Navigation Gaps ✅
- [x] UX.6.2 Leaderboard button: shows graceful "Signal Offline" placeholder for paid users

### UX.8 — Correct Swatch Dismiss ⬜
- [ ] UX.8.1 Dismiss animation for the correct swatch (pop / glow-burst / implode)

### Phase 8 — Firebase Leaderboard ⬜ (deferred — placeholder shown for paid users)
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

### Phase CI — CI/CD Pipeline ⬜ (workflows created, one-time setup required)

Workflows are in `.github/workflows/`. See `docs/ci_cd/CI_SETUP_CHECKLIST.md` for the full setup checklist.

**GitHub Secrets** (Settings → Secrets and variables → Actions → Secrets):
- [ ] CI.1 `PAT_HUEZOO_GITHUB` — Personal Access Token (`repo` scope + Variables read/write)
- [x] CI.2 `ANDROID_KEYSTORE_FILE` — base64-encoded `.jks` signing keystore
- [x] CI.3 `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`
- [x] CI.4 `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` — Google Play Console service account key
- [x] CI.5 `APPSTORE_KEY_ID`, `APPSTORE_ISSUER_ID`, `APPSTORE_PRIVATE_KEY` — App Store Connect API key
- [x] CI.6 `IOS_DIST_SIGNING_KEY_BASE64`, `IOS_DIST_SIGNING_KEY_PASSWORD` — Apple Distribution certificate
- [x] CI.7 `IOS_PROVISIONING_PROFILE_NAME` — App Store provisioning profile name

**GitHub Variables** (Settings → Secrets and variables → Actions → Variables):
- [ ] CI.8 `ANDROID_VERSION_CODE` = `1`
- [ ] CI.9 `IOS_BUILD_NUMBER` = `1`
- [ ] CI.10 `DEVELOPMENT_TEAM` = Apple Team ID (developer.apple.com → Membership)

**One-time iOS Xcode setup** (required before first TestFlight upload):
- [x] CI.11 Bundle Identifier set to `xyz.ksharma.huezoo`
- [x] CI.12 `CFBundleShortVersionString` and `CFBundleVersion` in `iosApp/iosApp/Info.plist`
- [ ] CI.13 Set `CURRENT_PROJECT_VERSION = 1` in Xcode → iosApp target → Build Settings
- [x] CI.14 Register `xyz.ksharma.huezoo` in App Store Connect

**Firebase** (configure after Phase 8):
- [ ] CI.15 Uncomment Firebase workflow steps in `build-android.yml`, `build-ios.yml`, `distribute-testflight.yml`
- [ ] CI.16 Add Firebase secrets (service account, app IDs, google-services files)

### Pre-Launch — Android ⬜
- [x] PL.A.1 AdMob App IDs confirmed production
- [x] PL.A.2 Signing keystore created; secrets added
- [x] PL.A.3 `versionCode` and `versionName` set in `build.gradle.kts`
- [ ] PL.A.4 ProGuard / R8 rules verified for release build — check SQLDelight, Koin, AdMob, Billing
- [ ] PL.A.5 Content rating questionnaire completed in Play Console
- [ ] PL.A.6 GDPR consent form configured in AdMob dashboard (required for EU users)
- [x] PL.A.7 Privacy Policy URL added to Play Store listing
- [ ] PL.A.8 Tested on at least one real Android device (release build)

### Pre-Launch — iOS ⬜
- [x] PL.I.1 Bundle Identifier set to `xyz.ksharma.huezoo`
- [x] PL.I.2 `CFBundleShortVersionString` and `CFBundleVersion` in `Info.plist`
- [x] PL.I.3 `NSUserTrackingUsageDescription` in `Info.plist`; ATT prompt wired
- [x] PL.I.4 App Store Connect record created
- [x] PL.I.5 Apple Distribution certificate + provisioning profile created; secrets added
- [ ] PL.I.6 StoreKit 2 IAP product configured in App Store Connect (currently stub)
- [ ] PL.I.7 TestFlight build uploaded and internal testing completed
- [x] PL.I.8 Privacy Policy URL added to App Store Connect listing
- [ ] PL.I.9 Tested on at least one real iOS device (release build)

### Pre-Launch — Common ⬜
- [x] PL.C.1 Privacy Policy live at https://ksharma-xyz.github.io/HueZoo/privacy-policy/
- [x] PL.C.2 App icon artwork complete — iOS PNGs + Android vector
- [x] PL.C.3 Font license review complete
- [ ] PL.C.4 All GitHub CI/CD secrets and variables configured (see Phase CI)

---

## Ship Checklist

- [x] Phase 7 (monetization) complete
- [x] UX.23 debug logs removed
- [x] Phase L font license review
- [x] App icon final artwork
- [x] Tested on real Android + iOS device
- [x] PL.A.1 AdMob IDs production
- [x] PL.I.3 NSUserTrackingUsageDescription
- [ ] PL.A.4 ProGuard rules verified
- [ ] PL.A.5 Content rating in Play Console
- [ ] PL.A.6 GDPR consent form (AdMob)
- [ ] PL.A.8 Real Android device — release build
- [ ] PL.I.6 StoreKit 2 IAP wired
- [ ] PL.I.7 TestFlight internal testing
- [ ] PL.I.9 Real iOS device — release build
- [ ] PL.C.4 CI/CD secrets + variables complete
