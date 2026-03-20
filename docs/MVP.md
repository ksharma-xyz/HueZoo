# Huezoo — MVP

*One obsessive game loop. Ship it. Hook people. Monetize the obsession.*

Implementation planning companion for LLM-driven feature work:
- `docs/IMPLEMENTATION_PLAN_LEVELS_ECONOMY_UX.md`

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
| DS.Font | Custom fonts: Bebas Neue + Clash Display + Space Grotesk (all loaded via composeResources/font/) | ✅ Done |
| DS.3 | Haptics — HapticEngine expect/actual (Android + iOS) | ⬜ |
| DS.4 | Sound — SoundEffect expect/actual, SoundPool / AVAudioPlayer | ⬜ |
| DS.5 | Animations — baked into each component as it's built | ⬜ |
| 1 | Color Math — rgbToLab, CIEDE2000, randomVividColor, seededColor | ✅ Done |
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

### Phase DS.Font — Custom Typography ✅
- [x] DS.Font.1 **Bebas Neue** (Regular) — display/numbers font. SIL OFL. Replaces Antonio.
- [x] DS.Font.2 **Clash Display** (Regular, Medium, SemiBold, Bold) — title/heading font. Fontshare FF EULA. Replaces Fredoka.
- [x] DS.Font.3 **Space Grotesk** (Regular, Medium, SemiBold, Bold) — body/label font. SIL OFL.
- [x] DS.Font.4 All `.ttf` files placed in `composeApp/src/commonMain/composeResources/font/` (snake_case)
- [x] DS.Font.5 `Typography.kt` updated — BebasNeue / ClashDisplay / SpaceGrotesk `FontFamily` objects wired to full type scale
- [ ] DS.Font.6 Verify previews render with correct fonts in Android Studio
- [ ] DS.Font.7 Verify on-device on Android + iOS simulator
- [ ] DS.Font.8 **License review** (see Phase L below before App Store submission)

### Phase DS.1 — Shapes & Effects
- [ ] DS.1.1 `SquircleShape` — `GenericShape` superellipse, variants: small/medium/large/card/button
- [ ] DS.1.2 `Modifier.colorGlow()` — platform actual: Android `setShadowLayer`, iOS `BlurMaskFilter`
- [ ] DS.1.3 `Modifier.depthShadow()` — layered Box for 3D card/button feel
- [ ] DS.1.4 `SwatchGradientOverlay` — top-left highlight + bottom-right shadow for physical chip look
- [ ] DS.1.5 **SwatchBlock shape exploration** — try hexagon (`HexagonShape` already in Shape.kt) or other non-squircle tile shapes. Current issue: `neonStrike` uses `drawRoundRect` which won't match a hexagon; need to port neon ring logic into `drawWithContent` using the actual shape path before switching. Keep squircle until this is polished.
- [ ] DS.1.6 **SkewedStatChip start padding** — text in the parallelogram chip sits too close to the left edge due to the skew angle; add ~3dp `paddingStart` offset so text clears the angled boundary cleanly.

### Phase DS.2 — Core Components
- [ ] DS.2.1 `HuezooButton` — primary/danger/ghost/score variants, 3D press (scale 0.94, shadow collapses)
- [ ] DS.2.2 `SwatchBlock` — sm/md/lg, states: default/pressed/correct/wrong/revealed + animations
- [ ] DS.2.3 `GameCard` — identity color accent bar, gradient tint, personal best, press animation
- [ ] DS.2.4 `DeltaEBadge` — color shifts cyan→yellow→magenta by difficulty, spring appear animation
- [ ] DS.2.5 `RoundIndicator` — dot row: inactive / active (pulse) / completed (green)
- [ ] DS.2.6 `ResultCard` — radial gradient, glow border, share-ready 1:1 layout
- [ ] DS.2.7 `HuezooBottomSheet` — game-styled sheet, 32dp top corners, handle bar

### Phase UX — UX Polish (do before haptics/sound/monetisation)

#### UX.1 — Result Screen
- [x] UX.1.1 Confetti burst on result enter (identity color particles, 50 count, gravity physics)
- [x] UX.1.2 Share button → native share sheet via `platformOps.shareText()`
- [x] UX.1.3 Sting copy on ResultCard based on ΔE achieved
- [x] UX.1.4 Daily result: show "Next puzzle in Xh Xm" countdown below buttons
- [x] UX.1.5 ResultCard slide-up entrance (offset 60dp, spring)
- [x] UX.1.6 Score count-up animation (0 → final, spring)

#### UX.2 — Home Screen Timers
- [x] UX.2.1 Daily card (completed): show live "Next puzzle in Xh Xm" countdown
- [x] UX.2.2 Threshold card (blocked): show live "Resets in Xh Xm" countdown

#### UX.3 — Already Played / Blocked States
- [x] UX.3.1 Daily already-played screen: styled "ALREADY PLAYED" + score + countdown + "BACK TO HOME" button
- [x] UX.3.2 Threshold blocked screen: show live reset countdown + styled "Back to Home" primary button

#### UX.4 — In-Game Feedback
- [x] UX.4.1 Correct tap: "↓ ΔE X.X — SHARPER" label via fixed feedback slot (graphicsLayer alpha, no layout shift)
- [x] UX.4.2 Daily: show today's date under "DAILY CHALLENGE" title ("March 20 · Same for everyone")
- [x] UX.4.3 Daily final round (6/6): feedback slot shows "Last one — make it count" on wrong tap

#### UX.5 — New User / Onboarding
- [ ] UX.5.1 First-launch ΔE info card on Home (dismissable, stored in SQLDelight `user_settings`)
- [ ] UX.5.2 Review subtitle copy on both game cards to be more instructional for new users

#### UX.6 — Navigation Gaps
- [ ] UX.6.1 Result "Play Again" for Threshold: check attempts before navigating — show inline "No tries left" if exhausted instead of starting a game that immediately blocks
- [ ] UX.6.2 Leaderboard button on Result: hide until Firebase is implemented (remove or show "Coming soon" toast)

#### UX.7 — Directional Feedback During Gameplay
*Problem: users don't know if lower ΔE is good or bad. No sense of direction or progress.*
- [ ] UX.7.1 On correct tap: show "↓ ΔE X.X — SHARPER" (gaming language, not just the number) for 700ms
- [ ] UX.7.2 DeltaEBadge tier label (below the number): "EASY" / "MODERATE" / "HARD" / "EXPERT" / "SUPERHUMAN" based on current ΔE range — updates each round
- [ ] UX.7.3 On level-up (ΔE crosses a tier boundary): brief tier-change pulse animation on DeltaEBadge (already partially specced DS.5) + "LEVEL UP" micro-text flash
- [ ] UX.7.4 First-round tooltip copy: "Lower ΔE = harder to spot. Go as low as you can." (one-time, dismisses on first tap)

#### UX.8 — Correct Swatch Dismiss Animation
*Make the correct tap feel viscerally satisfying.*
- [ ] UX.8.0 Design + implement a cool dismiss animation for the correct swatch (pop → scale to 0, or shatter, or glow-burst + implode — pick the most satisfying one). Consider: animate just the odd-colored swatch (the different one) vs both same-colored blocks.

#### UX.8 — Streak System
*Reward consecutive correct taps — make the player feel momentum.*
- [ ] UX.8.1 Track consecutive correct taps in ThresholdGameEngine (`correctStreak: Int`)
- [ ] UX.8.2 5-in-a-row: confetti burst (identity color particles) + "5 STREAK!" flash banner for 800ms
- [ ] UX.8.3 10-in-a-row: premium confetti (multi-color, higher density) + "UNSTOPPABLE!" banner + bonus gems awarded
- [ ] UX.8.4 Streak counter shown in HUD (appears after first correct, e.g. "🔥 3") — disappears on wrong tap
- [ ] UX.8.5 Streak breaks on wrong tap: brief "STREAK LOST" gray flash (no harsh punishment, just acknowledgement)

#### UX.9 — Gems Earning System
*Players need to see gems accumulate as they play — currency with meaning.*
- [ ] UX.9.1 Define gem earn rates: +1 per correct tap, +5 per round survived to round 5+, +10 per game completed, +50 bonus for 10-streak
- [ ] UX.9.2 On gem earn: animated "+N 💎" float-up label from the HUD gem counter, fades out over 600ms
- [ ] UX.9.3 HUD gem counter increments with a brief scale pulse (1.0 → 1.2 → 1.0, spring) when gems are added
- [ ] UX.9.4 Persist gem total in SQLDelight `user_stats` table (accumulated lifetime gems)
- [ ] UX.9.5 Result screen: show gems earned this session (e.g. "+32 💎 earned") below the stat cards

#### UX.10 — Home Screen & Splash Redesign
*Full redesign pass before ship — current design is functional but not polished enough.*
- [ ] UX.10.1 **Home screen redesign** — new layout from Stitch design, replace GameCard grid with more visual identity. Tackle after UX.5 and UX.6 are done.
- [ ] UX.10.2 **Splash screen** — animated intro (logo reveal, brand identity moment). Design + build after home screen is locked.
- [ ] UX.10.3 **Game levels / player progression** — define level thresholds based on ΔE achieved and score. Wire level badges into Result screen and Home. Full scope TBD in a dedicated design doc.

#### UX.11 — Result Screen Confetti Threshold
*Confetti should only fire when the result is actually worth celebrating.*
- [ ] UX.11.1 Define "good score" thresholds per game:
  - Threshold: confetti if best ΔE < 2.0 (player reached HARD territory)
  - Daily: confetti if all 6 rounds completed correctly (perfect run)
- [ ] UX.11.2 Pass a `showConfetti: Boolean` flag to `ResultScreen` from the nav result; compute in ViewModel before navigating
- [ ] UX.11.3 On bad/mediocre result: no confetti — keep the sting copy as the sole emotional beat

#### UX.12 — Scoring, Game Levels & Bonus Gems (post-game)
*Handle in one place — do not scatter across ViewModels.*
- [ ] UX.12.1 Create `GameRewardEngine` (commonMain) — takes `Result` and returns `RewardSummary(bonusGems, levelUp, newLevel)`
- [ ] UX.12.2 Define level thresholds and bonus gem amounts (separate design doc: `docs/GAME_LEVELS.md`)
- [ ] UX.12.3 Bonus gems awarded on Result screen entry — animated "+N" float from stat card, added to total
- [ ] UX.12.4 Ads strategy: show rewarded ad offer only if: (a) user is not paid, AND (b) score was good (≥ threshold), AND (c) ad not shown in last 24h. Never show after a bad result — that feels punishing.
- [ ] UX.12.5 All reward/ad/level logic flows through `GameRewardEngine` — neither ViewModel nor Screen contains raw logic

### Phase DS.3 — Haptics
- [ ] DS.3.1 `HapticType` enum in commonMain (Light, Medium, Heavy, Success, Error, Warning, Selection)
- [ ] DS.3.2 `HapticEngine` interface in commonMain
- [ ] DS.3.3 Android actual — `VibrationEffect` patterns per type (minSdk 28, safe to use all effects)
- [ ] DS.3.4 iOS actual — `UIImpactFeedbackGenerator` / `UINotificationFeedbackGenerator`
- [ ] DS.3.5 Koin binding — `androidModule` provides `AndroidHapticEngine(androidContext())`
- [ ] DS.3.6 Wire haptics into gameplay: wrong tap → `HapticType.Error` (medium buzz, not harsh — "teasing" not "punishing"); correct tap → `HapticType.Success` (light, satisfying); game over → `HapticType.Heavy`

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

### Phase 1 — Color Math ✅
- [x] 1.1 `rgbToLab(r, g, b)` + `Color.toLab()` — sRGB → CIELAB (D65), in `ColorMath.kt`
- [x] 1.2 `deltaE(lab1, lab2): Float` — full CIEDE2000 (Sharma 2005), in `ColorMath.kt`
- [x] 1.3 `randomVividColor(random)` — vivid gamut only (sat 65–100%, lig 30–70%), in `ColorEngine.kt`
- [x] 1.4 `generateOddSwatch(base, targetDeltaE, random): Color` — binary search in Lab a*/b* space, 22 iterations, in `ColorEngine.kt`
- [x] 1.5 `scoreFromDeltaE(de: Float): Int` — 1000/ΔE formula, floored at ΔE 0.3, in `ColorEngine.kt`
- [x] 1.6 `seededColorForDate(date: LocalDate): Color` — LCG hash of date, deterministic, in `ColorEngine.kt`
- [x] 1.7 `Lab.toColor()` — CIELAB → sRGB (inverse pipeline), sRGB gamut clamped, in `ColorMath.kt`

Files: `domain/color/Lab.kt`, `domain/color/ColorMath.kt`, `domain/color/ColorEngine.kt`

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
- [ ] 9.3 Haptic + sound tuning pass on real device
- [ ] 9.4 Test on real Android device + iOS device/simulator
- [ ] 9.5 Play Store listing — screenshots, description, content rating
- [ ] 9.6 App Store listing — screenshots, description, review submission

### Phase L — Font Licenses (before App Store submission)
- [ ] L.1 **Clash Display (Fontshare FF EULA)** — review full license at `Downloads/ClashDisplay_Complete/License/FFL.txt`. License explicitly permits "Mobile, Digital, Apps" commercial use. Verify bundling fonts inside an APK/IPA is acceptable under clause 01. If any concern: replace Clash Display with **DM Serif Display** or **Syne** (both SIL OFL, no restrictions).
- [ ] L.2 **Bebas Neue (SIL OFL 1.1)** — no action needed. OFL permits bundling in any software including commercial apps.
- [ ] L.3 **Space Grotesk (SIL OFL 1.1)** — no action needed.
- [ ] L.4 **Antonio (SIL OFL 1.1)** — no action needed (kept in font folder but no longer used in Typography.kt — can be removed).
- [ ] L.5 **Fredoka (SIL OFL 1.1)** — no action needed (same as above — kept but replaced by Clash Display).
- [ ] L.6 Add font attribution to app's "About" or Settings screen if required (Clash Display: "Designed by Indian Type Foundry").

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
