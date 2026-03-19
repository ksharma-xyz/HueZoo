# Huezoo — MVP

*One obsessive game loop. Ship it. Hook people. Monetize the obsession.*

---

## Progress

| Phase | What | Status |
|---|---|---|
| 0 | Gradle, SQLDelight, DI, nav structure, package layout | ✅ Done |
| Detekt | Code quality + Compose lint rules | ✅ Done |
| DS.0 | HuezooColors, HuezooTypography, HuezooTheme | ✅ Done |
| DS.0.5 | HuezooSpacing + HuezooSize dimension tokens | ✅ Done |
| DS.1 | SquircleShape, colorGlow modifier, depthShadow | ✅ Done |
| DS.2 | HuezooButton, SwatchBlock, GameCard, DeltaEBadge, RoundIndicator, ResultCard, BottomSheet | ✅ Done |
| DS.Font | Custom fonts loaded from composeResources (Space Grotesk + JetBrains Mono) | ⬜ |
| DS.3 | Haptics — HapticEngine expect/actual (Android + iOS) | ⬜ |
| DS.4 | Sound — SoundEffect expect/actual, SoundPool / AVAudioPlayer | ⬜ |
| DS.5 | Animations — baked into each component as it's built | ⬜ |
| 1 | Color Math — rgbToLab, CIEDE2000, randomVividColor, seededColor | ⬜ |
| 2 | Core UI components wired to color math | ⬜ |
| 3 | Home Screen | ⬜ |
| 4 | The Threshold — game loop | ⬜ |
| 5 | Daily Challenge — game loop | ⬜ |
| 6 | Result Screen | ⬜ |
| 7 | Monetization — AdMob + IAP | ⬜ |
| 8 | Firebase — leaderboard + anon auth | ⬜ |
| 9 | Polish + ship | ⬜ |

> DS.5 animations are not a separate pass — each animation lives inside the component it belongs to.
> Shake is part of SwatchBlock. Count-up is part of ResultCard. Stagger is part of HomeScreen.

---

## Core Idea

**The Threshold** — detect the smallest color difference you can. One miss, game over.
**Daily Challenge** — date-seeded puzzle, same for every player, 1 attempt per day.

---

## User Journey

```
Open app
  ├── Daily Challenge card  →  1 attempt/day  →  Result  →  Share
  └── The Threshold card    →  5 attempts / 8 hours
                                  ├── Out of tries → Watch Ad (+1) OR Unlock $2 forever
                                  └── Result → Leaderboard → Share
```

---

## Monetization

| Feature | Free | Paid ($2 one-time) |
|---|---|---|
| Daily Challenge | ✅ 1/day | ✅ 1/day |
| The Threshold attempts | 5 per 8 hours | ♾️ Unlimited |
| Watch ad for +1 try | ✅ | Not needed |
| Submit to leaderboard | ✅ | ✅ |
| Ads | ✅ shown | ❌ removed |

**Attempt window: 5 tries per 8 hours** (3 windows per day — more engagement than midnight reset).
`next_reset_at` stored as ISO timestamp in SQLDelight, not date string.

**Push toward $2 purchase:**
- Persistent "Unlock" button in Home top-right — always visible
- After 3rd try used: "X of 5 tries left" counter on Threshold card
- After 5th try: Paywall sheet slides up automatically
- Paywall: `HuezooButton` primary = "Unlock Forever — $2", ghost secondary = "Watch Ad (+1 try)"
- After purchase: remove attempt cap, hide ads, show "Unlimited" badge on Threshold card

---

## Tech Stack

| Layer | Choice |
|---|---|
| UI | Compose Multiplatform (Android + iOS) |
| Navigation | Navigation3 (KMP, alpha06) |
| Local DB | SQLDelight 2.3.2 |
| Backend | Firebase Realtime Database (leaderboard only) |
| Auth | Firebase Anonymous Auth |
| Ads | AdMob (Android + iOS) |
| IAP | Google Play Billing + StoreKit 2 (iOS) |
| Color Math | Pure Kotlin (commonMain) |
| Share | PlatformOps interface — Android Intent / iOS UIActivityViewController |

---

## App Structure

```
Home Screen
  ├── [Unlock] button (top-right, always visible)
  ├── Daily Challenge card  (date, done/not done, yesterday score, countdown)
  └── The Threshold card    (your best ΔE, your rank, tries remaining)

Game: The Threshold
  └── 3 SwatchBlocks → tap odd one → ΔE tightens → miss = game over

Game: Daily Challenge
  └── Date-seeded puzzle, same for all users, 1 attempt, share card

Result Screen
  ├── Animated ScoreCard (slide up + confetti)
  ├── [Play Again]  — checks attempt gate
  ├── [Share]       — native share sheet
  └── [Leaderboard] — view top 50

Leaderboard Screen
  └── Top 50, rank by lowest ΔE, your entry highlighted

Paywall Sheet
  ├── [Unlock Forever — $2]   ← primary CTA
  └── [Watch Ad → +1 try]     ← ghost secondary
```

---

## Detailed Task List

### Phase 0 — Project Setup ✅
- [x] 0.1 Review existing code, rename FunWithColors → Huezoo everywhere
- [x] 0.2 Set up Navigation3 (KMP) — `NavDisplay` in `App.kt`
- [x] 0.3 Package structure: `ui/home`, `ui/games/threshold`, `ui/games/daily`, `ui/result`, `ui/leaderboard`, `ui/paywall`, `domain/color`, `data/db`, `platform/`
- [x] 0.4 SQLDelight schema — `daily_challenge`, `threshold_session` (8h window), `personal_best`, `user_settings`
- [x] 0.5 DB driver factory interface + Android/iOS actuals (KRAIL pattern)
- [x] 0.6 Koin DI — `appModule` + `platformDatabaseModule` expect/actual
- [x] 0.7 `PlatformOps` — `shareText()` Android + iOS implementations
- [x] 0.8 iOS app renamed to HueZoo (Config.xcconfig)
- [x] 0.9 Detekt setup — `config/detekt.yml` with Compose rules, applied in composeApp

### Phase DS.0 — Design System Foundation ✅
- [x] DS.0.1 `HuezooColors` — full dark game palette (Background, SurfaceL1-L3, accents, glows, game identity colors)
- [x] DS.0.2 `HuezooTypography` — Space Grotesk type scale (fonts: drop TTF files into `composeResources/font/` to activate)
- [x] DS.0.3 `HuezooTheme` — `MaterialTheme` wrapper with dark color scheme + typography

### Phase DS.Font — Custom Typography (separate track)
- [ ] DS.Font.1 Download **Space Grotesk** (4 weights: Regular, Medium, SemiBold, Bold) from fonts.google.com/specimen/Space+Grotesk
- [ ] DS.Font.2 Download **JetBrains Mono** (Regular, Medium) from fonts.google.com/specimen/JetBrains+Mono — used for ΔE badge and numeric readouts
- [ ] DS.Font.3 Place all `.ttf` files in `composeApp/src/commonMain/composeResources/font/` (snake_case filenames)
- [ ] DS.Font.4 Run `./gradlew generateCommonMainResourceAccessors` to generate `Res.font.*` accessors
- [ ] DS.Font.5 Update `Typography.kt` — replace `FontFamily.SansSerif` with `spaceGroteskFamily()` and `FontFamily.Monospace` with `jetBrainsMonoFamily()` loaded via `Font(Res.font.X, weight)`
- [ ] DS.Font.6 Update `DeltaEBadge` — `labelSmall` (JetBrains Mono) for the ΔE number display
- [ ] DS.Font.7 Verify previews render with correct fonts in Android Studio
- [ ] DS.Font.8 Verify on-device on Android + iOS simulator

> **How to use:** Run `/kmp-typography` skill when activating fonts — it has the full setup guide
> including FontFamily construction, Typography.kt structure, and troubleshooting.

### Phase DS.1 — Shapes & Effects
- [ ] DS.1.1 `SquircleShape` — `GenericShape` superellipse, variants: small/medium/large/card/button
- [ ] DS.1.2 `Modifier.colorGlow()` — platform actual: Android `setShadowLayer`, iOS `BlurMaskFilter`
- [ ] DS.1.3 `Modifier.depthShadow()` — layered Box for 3D card/button feel
- [ ] DS.1.4 `SwatchGradientOverlay` — top-left highlight + bottom-right shadow for physical chip look

### Phase DS.2 — Core Components
- [ ] DS.2.1 `HuezooButton` — primary/danger/ghost/score variants, 3D press (scale 0.94, shadow collapses)
- [ ] DS.2.2 `SwatchBlock` — sm/md/lg, states: default/pressed/correct/wrong/revealed + animations
- [ ] DS.2.3 `GameCard` — identity color accent bar, gradient tint, personal best, press animation
- [ ] DS.2.4 `DeltaEBadge` — color shifts cyan→yellow→magenta by difficulty, spring appear animation
- [ ] DS.2.5 `RoundIndicator` — dot row: inactive / active (pulse) / completed (green)
- [ ] DS.2.6 `ResultCard` — radial gradient, glow border, share-ready 1:1 layout
- [ ] DS.2.7 `HuezooBottomSheet` — game-styled sheet, 32dp top corners, handle bar

### Phase DS.3 — Haptics
- [ ] DS.3.1 `HapticType` enum in commonMain (Light, Medium, Heavy, Success, Error, Warning, Selection)
- [ ] DS.3.2 `HapticEngine` interface in commonMain
- [ ] DS.3.3 Android actual — `VibrationEffect` patterns per type (minSdk 28, safe to use all effects)
- [ ] DS.3.4 iOS actual — `UIImpactFeedbackGenerator` / `UINotificationFeedbackGenerator`
- [ ] DS.3.5 Koin binding — `androidModule` provides `AndroidHapticEngine(androidContext())`

### Phase DS.4 — Sound
- [ ] DS.4.1 Source 8 sound files: `correct`, `wrong`, `levelup`, `gameover`, `tick`, `confetti`, `button_tap`, `swatch_tap`
- [ ] DS.4.2 `SoundEffect` enum in commonMain
- [ ] DS.4.3 `SoundPlayer` interface in commonMain
- [ ] DS.4.4 Android actual — `SoundPool`, pre-load all sounds on init
- [ ] DS.4.5 iOS actual — `AVAudioPlayer`
- [ ] DS.4.6 User setting: sound on/off (default off), stored in SQLDelight `user_settings`

### Phase DS.5 — Animations (baked into components)
- [ ] DS.5.1 `ShakeState` + `Modifier.shake()` — for SwatchBlock wrong answer
- [ ] DS.5.2 `ConfettiEffect` composable — 50 particles, physics, identity color mix
- [ ] DS.5.3 `animatedFloat()` / `animatedInt()` — count-up for ResultCard score
- [ ] DS.5.4 `Modifier.staggeredFadeUp(index)` — entrance animation for HomeScreen cards
- [ ] DS.5.5 Spring scale appear on SwatchBlock (0.85 → 1.0)
- [ ] DS.5.6 Correct answer pulse (1.0 → 1.08 → 1.0 + green border flash)
- [ ] DS.5.7 ResultCard slide-up entrance (offset 60dp + scale 0.9 → 1.0, spring)

### Phase 1 — Color Math
- [ ] 1.1 `rgbToLab(r, g, b)` — sRGB → CIELAB conversion
- [ ] 1.2 `deltaE(lab1, lab2): Float` — CIEDE2000 formula
- [ ] 1.3 `randomVividColor(): Color` — avoid near-black/white/grey, vivid gamut only
- [ ] 1.4 `generateOddSwatch(base, targetDeltaE): Pair<Color, Color>` — base + odd at exact ΔE
- [ ] 1.5 `scoreFromDeltaE(de: Float): Int` — shared scoring formula
- [ ] 1.6 `seededColorForDate(date: LocalDate): Color` — deterministic daily color from date hash

### Phase 2 — Core UI Components wired to game logic
- [ ] 2.1 `SwatchBlock` wired to tap events and game state
- [ ] 2.2 `ScoreCard` with count-up and confetti
- [ ] 2.3 `GameCard` reading real data from SQLDelight (personal best, rank)
- [ ] 2.4 `DeltaEBadge` live updates during game
- [ ] 2.5 `RoundIndicator` wired to game round state

### Phase 3 — Home Screen
- [ ] 3.1 Two `GameCard`s: Daily Challenge + The Threshold
- [ ] 3.2 Daily card: today's date, "Done ✓" if played, yesterday's score, countdown to reset
- [ ] 3.3 Threshold card: personal best ΔE, rank, tries remaining in current 8h window
- [ ] 3.4 Persistent "Unlock" `IconButton` top-right (navigates to paywall)
- [ ] 3.5 Staggered entrance animation (80ms delay per card)
- [ ] 3.6 Tap → navigate to game

### Phase 4 — The Threshold Game
- [ ] 4.1 `ThresholdViewModel` — game state: current ΔE (start 5.0), round, attempts
- [ ] 4.2 Start at ΔE 5.0, step down 0.3 per correct pick
- [ ] 4.3 Show 3 `SwatchBlock`s — 2 base + 1 odd, positions shuffled each round
- [ ] 4.4 Correct tap → green pulse, next round, show `DeltaEBadge`
- [ ] 4.5 Wrong tap → shake animation, game over, navigate to Result
- [ ] 4.6 Gate check on game start: `attempts_used >= 5` AND not paid → show `PaywallSheet`
- [ ] 4.7 Deduct 1 attempt from SQLDelight on game start
- [ ] 4.8 Show remaining attempts count during game

### Phase 5 — Daily Challenge Game
- [ ] 5.1 `DailyViewModel` — load `seededColorForDate(today)` as base
- [ ] 5.2 6 rounds, fixed ΔE curve (e.g. 4.0, 3.0, 2.0, 1.5, 1.0, 0.7) — same for all players
- [ ] 5.3 Block replay if `completed = 1` for today in SQLDelight
- [ ] 5.4 Save score + mark completed in SQLDelight on finish
- [ ] 5.5 Countdown timer until tomorrow (shown on Home card after completion)

### Phase 6 — Result Screen
- [ ] 6.1 `ResultCard` slides up (offset + scale spring)
- [ ] 6.2 Count-up animation for final ΔE and score
- [ ] 6.3 `ConfettiEffect` burst on enter
- [ ] 6.4 Sting copy based on ΔE achieved (see DESIGN_SYSTEM.md copy pool)
- [ ] 6.5 Percentile label "Better than X% of players" (from Firebase query)
- [ ] 6.6 [Play Again] → re-checks attempt gate
- [ ] 6.7 [Share] → `platformOps.shareText("I detected ΔE 1.2 — top 6% | Huezoo")`
- [ ] 6.8 [View Leaderboard] → navigate to LeaderboardScreen

### Phase 7 — Monetization
- [ ] 7.1 Attempt counter in Threshold game ("X of 5 tries used")
- [ ] 7.2 `PaywallSheet` — full implementation with primary $2 CTA + ghost ad option
- [ ] 7.3 AdMob setup — rewarded ad for +1 try (Android + iOS)
- [ ] 7.4 IAP setup — one-time "Unlimited" product (Google Play Billing + StoreKit 2)
- [ ] 7.5 Persist `is_paid = true` in SQLDelight `user_settings` on purchase
- [ ] 7.6 If paid: hide ads, remove attempt cap, show "Unlimited" badge

### Phase 8 — Firebase Leaderboard
- [ ] 8.1 Firebase project setup, enable Realtime DB + Anonymous Auth
- [ ] 8.2 Firebase config — Android `google-services.json`, iOS `GoogleService-Info.plist`
- [ ] 8.3 Add `firebase-gitlive` KMP SDK to libs.versions.toml (see KRAIL reference)
- [ ] 8.4 Schema: `/leaderboard/{uid}: { name, deltaE, timestamp }`
- [ ] 8.5 Query: order by `deltaE` ascending, limit 50
- [ ] 8.6 `LeaderboardScreen` — ranked list, your entry highlighted
- [ ] 8.7 Submit flow: name input sheet (stored locally after first entry) → push to Firebase
- [ ] 8.8 Security rules: public read, anon-auth write, max 1 entry per UID

### Phase 9 — Polish & Ship
- [ ] 9.1 App icon (all sizes) + splash screen
- [ ] 9.2 System dark mode — already dark-only; verify on light-mode devices
- [ ] 9.3 Space Grotesk fonts loaded via `composeResources/font/`
- [ ] 9.4 Haptic + sound tuning pass on real device
- [ ] 9.5 Test on real Android device + iOS device/simulator
- [ ] 9.6 Play Store listing — screenshots, description, content rating
- [ ] 9.7 App Store listing — screenshots, description, review submission

---

## Scoring: The Threshold

```
Starting ΔE:  5.0
Step down:    −0.3 per correct pick
Game over:    first miss

Leaderboard rank:  lowest threshold ΔE (ascending — lower = sharper eyes)
Your threshold:    the ΔE value you missed at
```

---

## Animations Spec

| Trigger | Animation | Duration |
|---|---|---|
| Home cards enter | Stagger fade-up, 80ms delay per card | 300ms each |
| SwatchBlock appear | Scale 0.85 → 1.0, spring | 250ms |
| Correct tap | Scale 1.0 → 1.08 → 1.0 + green border flash | 300ms |
| Wrong tap | ShakeX ±10dp × 3 cycles + magenta border | 300ms |
| ΔE badge appear | Scale 0.5 → 1.0 + fade in, spring | 400ms |
| Score count-up | Int lerp 0 → final, spring | 800ms |
| Result card enter | Slide up 60dp + scale 0.9 → 1.0, spring | 450ms |
| Confetti | 50 particles, gravity 0.4, spin random | 2000ms |
| Button press | Scale 1.0 → 0.94 instantly | 80ms |
| Button release | Scale 0.94 → 1.0, spring | 200ms |

---

## Share Card Format

```
I detected ΔE 1.2 on The Threshold
Better than 94% of players

Play Huezoo — huezoo.app
```

---

## Design System Reference

See `docs/DESIGN_SYSTEM.md` for:
- Full color palette with hex values
- Typography scale (Space Grotesk)
- Component specs: HuezooButton, SwatchBlock, GameCard, DeltaEBadge, etc.
- Haptic patterns per event
- The Sting Principle — copy tone guide for all in-app messages
