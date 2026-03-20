# Huezoo — MVP

*One obsessive game loop. Ship it. Hook people. Monetize the obsession.*

Game rules, scoring, gem economy, and ΔE tiers are the single source of truth in:
- `docs/GAME_DESIGN.md`

---

## Progress

| Phase | What | Status |
|---|---|---|
| 0 | Gradle, SQLDelight, DI, nav structure, package layout | ✅ Done |
| Detekt | Code quality + Compose lint rules | ✅ Done |
| DS.0 | HuezooColors, HuezooTypography, HuezooTheme | ✅ Done |
| DS.0.5 | HuezooSpacing + HuezooSize dimension tokens | ✅ Done |
| DS.1 | Shapes, shadows, overlays | ✅ Done |
| DS.2 | Core components: Button, SwatchLayout, GameCard, BottomSheet, TopBar, CurrencyPill | ✅ Done |
| DS.Font | Custom fonts: Bebas Neue + Clash Display + Space Grotesk | ✅ Done |
| DS.3 | Haptics — HapticEngine expect/actual (Android + iOS) | ⬜ |
| DS.4 | Sound — SoundEffect expect/actual, SoundPool / AVAudioPlayer | ⬜ |
| DS.5 | Animations — confetti, shake, count-up, spring scale, slide-up | ✅ Done |
| 1 | Color Math — rgbToLab, CIEDE2000, randomVividColor, seededColor | ✅ Done |
| 2 | Core UI components wired to color math | ✅ Done |
| 3 | Home Screen | ✅ Done |
| 4 | The Threshold — game loop | ✅ Done |
| 5 | Daily Challenge — game loop | ✅ Done |
| 6 | Result Screen | ✅ Done |
| 7 | Monetization — AdMob + IAP | ⬜ |
| 8 | Firebase — leaderboard + anon auth | ⬜ |
| 9 | Polish + ship | ⬜ |

---

## Core Idea

**The Threshold** — detect the smallest color difference you can. Try budget = 10 tries per 8h window. Best ΔE across all tries is your result.
**Daily Challenge** — date-seeded puzzle, same for every player, 6 rounds always played, 1 attempt per day.

---

## User Journey

```
Open app
  ├── Daily Challenge card  →  1 attempt/day  →  Result  →  Share
  └── The Threshold card    →  10 tries / 8 hours
                                  ├── Out of tries → Watch Ad (+1) OR Unlock $2 forever
                                  └── Result → Share
```

---

## Monetization

| Feature | Free | Paid ($2 one-time) |
|---|---|---|
| Daily Challenge | ✅ 1/day | ✅ 1/day |
| The Threshold attempts | 10 per 8 hours | ♾️ Unlimited |
| Watch ad for +1 try | ✅ | Not needed |
| Submit to leaderboard | ✅ | ✅ |
| Ads | ✅ shown | ❌ removed |

**Attempt window: 10 tries per 8 hours.**
`next_reset_at` stored as ISO timestamp in SQLDelight, not date string.

---

## Tech Stack

| Layer | Choice |
|---|---|
| UI | Compose Multiplatform (Android + iOS) |
| Navigation | Navigation3 (KMP) |
| Local DB | SQLDelight 2.x |
| Backend | Firebase Realtime Database (leaderboard only) |
| Auth | Firebase Anonymous Auth |
| Ads | AdMob (Android + iOS) |
| IAP | Google Play Billing + StoreKit 2 (iOS) |
| Color Math | Pure Kotlin (commonMain) |
| Share | PlatformOps interface — Android Intent / iOS UIActivityViewController |

---

## Detailed Task List

### Phase 0 — Project Setup ✅
- [x] 0.1 Review existing code, rename FunWithColors → Huezoo everywhere
- [x] 0.2 Set up Navigation3 (KMP) — `NavDisplay` in `App.kt`
- [x] 0.3 Package structure: `ui/home`, `ui/games/threshold`, `ui/games/daily`, `ui/result`, `ui/leaderboard`, `ui/paywall`, `domain/color`, `data/db`, `platform/`
- [x] 0.4 SQLDelight schema — `daily_challenge`, `threshold_session` (8h window), `personal_best`, `user_settings`
- [x] 0.5 DB driver factory interface + Android/iOS actuals
- [x] 0.6 Koin DI — `appModule` + `platformDatabaseModule` expect/actual; iOS `doInitKoin()` called in `iOSApp.init()`
- [x] 0.7 `PlatformOps` — `shareText()` Android + iOS implementations
- [x] 0.8 iOS app renamed to HueZoo; Xcode linker flags fixed (`-ObjC -lsqlite3`, `FRAMEWORK_SEARCH_PATHS`)
- [x] 0.9 Detekt setup — `config/detekt.yml` with Compose rules, applied in composeApp

### Phase DS.0 — Design System Foundation ✅
- [x] DS.0.1 `HuezooColors` — full dark game palette (Background, SurfaceL1-L3, accents, glows, game identity colors, GemGreen)
- [x] DS.0.2 `HuezooTypography` — Space Grotesk + Bebas Neue + Clash Display type scale
- [x] DS.0.3 `HuezooTheme` — `MaterialTheme` wrapper with dark color scheme + typography

### Phase DS.Font — Custom Typography ✅
- [x] DS.Font.1 **Bebas Neue** (Regular) — display/numbers font. SIL OFL.
- [x] DS.Font.2 **Clash Display** (Regular, Medium, SemiBold, Bold) — title/heading font. Fontshare FF EULA.
- [x] DS.Font.3 **Space Grotesk** (Regular, Medium, SemiBold, Bold) — body/label font. SIL OFL.
- [x] DS.Font.4 All `.ttf` files placed in `composeApp/src/commonMain/composeResources/font/`
- [x] DS.Font.5 `Typography.kt` updated — full type scale wired
- [ ] DS.Font.6 Verify previews render with correct fonts in Android Studio
- [ ] DS.Font.7 Verify on-device on Android + iOS simulator
- [ ] DS.Font.8 **License review** — see Phase L below before App Store submission

### Phase DS.1 — Shapes & Effects ✅
- [x] DS.1.1 Shape variants — `SquircleSmall`, `SquircleMedium`, `HexagonSwatch`, `DiamondSwatch`, `SwatchPetal`, `ParallelogramBack`
- [x] DS.1.2 `Modifier.shapedShadow()` — neo-brutalist offset shadow for any shape
- [x] DS.1.3 `Modifier.rimLight()` — top-left inset highlight for depth
- [x] DS.1.4 `SwatchGradientOverlay` — top-left highlight + bottom-right shadow for physical chip look
- [x] DS.1.5 5 swatch layout styles — `Flower`, `HexRing`, `SquircleOrbit`, `SpokeBlades`, `DiamondHalo`; random style per round
- [x] DS.1.6 `AmbientGlowBackground` — radial gradient glow behind game content

### Phase DS.2 — Core Components ✅
- [x] DS.2.1 `HuezooButton` — Primary / Ghost / GhostDanger variants; 3D press with shelf shadow
- [x] DS.2.2 `RadialSwatchLayout` — 6 tiles in a ring, unfold/fold spring animations, shake on wrong, neon border on correct/wrong/revealed; `SwatchSize` enum (Normal / Medium 1.2×)
- [x] DS.2.3 `GameCard` composables on Home — Threshold card (scanner illustration) + Daily card (gradient + round indicators)
- [x] DS.2.4 `SkewedStatChip` — parallelogram stat badge used in game HUD
- [x] DS.2.5 `HuezooTopBar` — glassmorphic bar with back button, wordmark, `CurrencyPill`, optional `?` help button
- [x] DS.2.6 `CurrencyPill` — parallelogram gem counter with slot-machine `AnimatedContent` + scale pulse on gem change
- [x] DS.2.7 `HuezooBottomSheet` — game-styled modal sheet, 32dp top corners, handle bar
- [x] DS.2.8 `GameHelpSheet` — `ThresholdHelpSheet` + `DailyHelpSheet`; triggered by `?` button in top bar

### Phase DS.3 — Haptics ⬜
- [ ] DS.3.1 `HapticType` enum in commonMain (Light, Medium, Heavy, Success, Error, Warning, Selection)
- [ ] DS.3.2 `HapticEngine` interface in commonMain
- [ ] DS.3.3 Android actual — `VibrationEffect` patterns per type
- [ ] DS.3.4 iOS actual — `UIImpactFeedbackGenerator` / `UINotificationFeedbackGenerator`
- [ ] DS.3.5 Koin binding
- [ ] DS.3.6 Wire into gameplay: wrong tap → Error; correct tap → Success; game over → Heavy

### Phase DS.4 — Sound ⬜
- [ ] DS.4.1 Source sound files: `correct`, `wrong`, `gameover`, `tick`, `confetti`, `button_tap`
- [ ] DS.4.2 `SoundEffect` enum + `SoundPlayer` interface in commonMain
- [ ] DS.4.3 Android actual — `SoundPool`; iOS actual — `AVAudioPlayer`
- [ ] DS.4.4 User setting: sound on/off (default off), stored in SQLDelight `user_settings`

### Phase DS.5 — Animations ✅
- [x] DS.5.1 Shake animation on `RadialSwatchLayout` wrong tile (±10dp × 3 cycles)
- [x] DS.5.2 `ConfettiEffect` composable — 117 particles, projectile physics, 5 shapes, identity color mix; fires on `gemsEarned > 0`
- [x] DS.5.3 Gem count-up animation on Result screen (0 → final, spring, `Animatable`)
- [x] DS.5.4 Staggered unfold on swatch layout (70ms per tile, spring)
- [x] DS.5.5 Spring scale appear on swatch tiles (0 → 1, `DampingRatioMediumBouncy`)
- [x] DS.5.6 Correct / wrong / revealed neon border via `drawWithContent` + shape path
- [x] DS.5.7 ResultScreen slide-up entrance (60dp offset + spring) + fade-in
- [x] DS.5.8 `GemBreakdownCard` staggered fade+slide (500ms base delay, +120ms per item)

### Phase 1 — Color Math ✅
- [x] 1.1 `rgbToLab` + `Color.toLab()` — sRGB → CIELAB (D65)
- [x] 1.2 `deltaE(lab1, lab2)` — full CIEDE2000 (Sharma 2005)
- [x] 1.3 `randomVividColor()` — vivid gamut only (sat 65–100%, lig 30–70%)
- [x] 1.4 `generateOddSwatch(base, targetDeltaE)` — binary search in Lab a*/b* space, 22 iterations
- [x] 1.5 `scoreFromDeltaE(de)` — 1000/ΔE formula, floored at ΔE 0.3
- [x] 1.6 `seededColorForDate(date)` — LCG hash of date, deterministic, same for all players
- [x] 1.7 `Lab.toColor()` — CIELAB → sRGB inverse pipeline

### Phase 2 — Core UI Components ✅
- [x] 2.1 `RadialSwatchLayout` wired to tap events and game state (correct / wrong / revealed display states)
- [x] 2.2 Result screen hero gems count-up + gem breakdown card
- [x] 2.3 Home screen game cards reading real data from SQLDelight (personal best ΔE, tries remaining, completion state)
- [x] 2.4 `SkewedStatChip` live updates during game (TAP count, TRIES remaining, CURRENT ΔE)
- [x] 2.5 Round indicators on Daily card (dot row: played / active / upcoming)

### Phase 3 — Home Screen ✅
- [x] 3.1 Two game cards: Daily Challenge + The Threshold
- [x] 3.2 Daily card: today's date, completion state, countdown to next puzzle
- [x] 3.3 Threshold card: personal best ΔE, tries remaining, live "Resets in Xh Xm" countdown
- [x] 3.4 Gem inventory panel with `GemSpillIllustration` watermark + baseline-aligned number + label
- [x] 3.5 Stat boxes: STREAK (days) + RANK (placeholder until Firebase)
- [x] 3.6 Player level badge + progress bar toward next level
- [x] 3.7 `LifecycleResumeEffect` — gem count refreshes on return from game

### Phase 4 — The Threshold ✅
- [x] 4.1 `ThresholdViewModel` — try-budget model (10 tries / 8h window), tap counter, ΔE progression
- [x] 4.2 Start at ΔE 5.0, step −0.3 per correct tap, floor ΔE 0.1
- [x] 4.3 6 swatches in `RadialSwatchLayout` — random layout style per round, random base color per tap
- [x] 4.4 Correct tap → green neon border, fold + new round, ΔE tightens
- [x] 4.5 Wrong tap → shake + magenta border + sting copy, try burned, ΔE resets to 5.0
- [x] 4.6 Gate check on screen enter: `Exhausted` → `ThresholdUiState.Blocked` screen with countdown
- [x] 4.7 Attempt recorded in SQLDelight on wrong tap
- [x] 4.8 HUD: TAP counter + TRIES REMAINING chip + CURRENT ΔE chip
- [x] 4.9 Gem earn: +2 per correct tap; milestone bonuses (ΔE < 2.0 → +5, < 1.0 → +10, < 0.5 → +25); milestones reset per try
- [x] 4.10 `GameRewardRates` — pure constants object; single source of truth for all gem rates
- [x] 4.11 `?` help button in top bar → `ThresholdHelpSheet` bottom sheet

### Phase 5 — Daily Challenge ✅
- [x] 5.1 `DailyViewModel` — loads `seededColorForDate(today)` as base; fixed ΔE curve [4.0, 3.0, 2.0, 1.5, 1.0, 0.7]
- [x] 5.2 6 rounds always played — wrong tap reveals correct swatch and advances (does not end game)
- [x] 5.3 Block replay: `AlreadyPlayed` state if `completed = 1` for today
- [x] 5.4 Save score + mark completed in SQLDelight on finish
- [x] 5.5 Gem earn: +5 per correct round, +3 participation, +20 perfect-run bonus (all 6 correct)
- [x] 5.6 `?` help button in top bar → `DailyHelpSheet` bottom sheet

### Phase 6 — Result Screen ✅
- [x] 6.1 Slide-up entrance (60dp offset, spring) + fade-in
- [x] 6.2 Hero: `+N GEMS` animated count-up (AccentCyan, italic display font)
- [x] 6.3 `GemBreakdownCard` — per-line staggered fade+slide (500ms base delay, +120ms per item)
- [x] 6.4 Stat cards: BEST ΔE + TAPS (Threshold) / ROUNDS CORRECT (Daily)
- [x] 6.5 `StingReadout` — ΔE value + tier badge + sting copy (varies by game + ΔE range)
- [x] 6.6 Outcome banner: "MISSION OUTCOME: COMPLETE / FAILURE / FLATLINED"
- [x] 6.7 Confetti fires when `gemsEarned > 0`; zero-gem result shows danger (AccentMagenta hero)
- [x] 6.8 [Play Again] — enabled only if `canPlayAgain`; shows "NO TRIES LEFT" ghost-danger when exhausted
- [x] 6.9 [Share] → `platformOps.shareText()`
- [x] 6.10 Daily: "Next puzzle in Xh Xm" countdown below buttons
- [ ] 6.11 "NEW PERSONAL BEST" badge — `isNewPersonalBest` is in `ResultUiState.Ready` but not yet surfaced in the UI

### Phase UX — Pending Items

#### UX.5 — Onboarding ⬜
- [x] UX.5.0 In-game `?` help button on Threshold + Daily screens with rules bottom sheet
- [ ] UX.5.1 First-launch onboarding — 3-slide walkthrough before Home; gated on `SettingsRepository.hasSeenOnboarding()` flag; slide 1: ΔE explainer, slide 2: Threshold rules, slide 3: Daily rules; skip always visible
- [ ] UX.5.2 Review subtitle copy on both game cards to be more instructional for new users
- [ ] UX.5.3 **Eye strain / health notice** — displayed on first launch (slide 0, before ΔE explainer) and accessible from About / Settings at any time. Copy must follow store guidelines — no fear-based language. Suggested wording: *"This game exercises your colour perception. Extended play may cause eye fatigue. Take breaks and stop if you experience discomfort. Play at your own pace."* Implementation notes: same `hasSeenOnboarding()` gate; include a "Got it" dismiss; also surface in About section (UX.16).

#### UX.6 — Navigation Gaps ⬜
- [x] UX.6.1 Result "Play Again" for Threshold: `canPlayAgain` checked; shows "NO TRIES LEFT" disabled button if exhausted
- [ ] UX.6.2 Leaderboard button on Result: hide until Firebase is implemented

#### UX.7 — Directional Feedback ⬜
- [x] UX.7.1 Correct tap feedback: "↓ ΔE X.X — SHARPER" in fixed-height slot (graphicsLayer alpha, no layout shift)
- [ ] UX.7.2 ΔE tier label in HUD (below ΔE chip): "BEGINNER / TRAINING / SHARP / EXPERT / ELITE" — updates each round
- [ ] UX.7.3 Tier-change animation: brief pulse when ΔE crosses a tier boundary
- [ ] UX.7.4 First-round tooltip: "Lower ΔE = harder to spot" — one-time, dismisses on first tap
- [ ] UX.7.5 **Daily Challenge: per-round base color variety** — currently all 6 rounds use the same hue family; each round should seed a different base color so the challenge feels visually distinct across rounds. Wire through `DailyGameEngine` / `DefaultDailyGameEngine` date+round seed.

#### UX.8 — Satisfying Correct Dismiss ⬜
- [ ] UX.8.1 Design + implement dismiss animation for the correct swatch (pop/glow-burst/implode)

#### UX.9 — Gem Float-Up ⬜
- [x] UX.9.1 Gem earn rates defined in `GameRewardRates`; per-session tracking in both ViewModels
- [x] UX.9.2 `CurrencyPill` scale pulse (1.0 → 1.2 → 1.0, spring) when gem count changes on return to Home
- [ ] UX.9.3 In-game "+N 💎" float-up label from HUD gem counter on earn (fades out over 600ms)

#### UX.10 — Progression Screens ✅
- [x] UX.10.1 Home screen — full design with gem panel, player level, stat boxes, game cards, illustrations
- [x] UX.10.2 **Levels & Progress sheet** — `LevelsProgressSheet.kt`; tap gem inventory area on Home → shows all 5 tiers as color-coded neo-brutalist cards, progress bar, gem threshold to next level. Design ref: `docs/stitch_huezoo_prd_design_doc/huezoo_levels_progress/`
- [x] UX.10.3 **Splash screen** — `SplashScreen.kt`; tactical scanner illustration (dot grid, arcs, crosshair, corner brackets, scan lines) + "HUEZOO" 96sp Bebas Neue italic springs in + "IDENTIFY THE OUTLIER" tagline; ~2.3 s then cross-fades to Home. `Splash` destination removed from back stack on exit.

#### UX.11 — Out of Tries Refill Sheet ⬜
*Replace current full-screen Blocked state with a monetisation-ready modal.*
- [ ] UX.11.1 **Refill bottom sheet** — two options: gem refill (300 gems for 10 tries, disabled + warning if insufficient) + Watch Ad (free, always available stub). Design ref: `docs/stitch_huezoo_prd_design_doc/huezoo_refill_out_of_tries/`
- [ ] UX.11.2 Gem deduction via `SettingsRepository.addGems(-300)` on gem refill
- [ ] UX.11.3 Wire Watch Ad button stub (no-op until AdMob Phase 7)

#### UX.12 — Streak System ⬜
- [ ] UX.12.1 Track consecutive correct taps in `ThresholdViewModel` (`correctStreak: Int`)
- [ ] UX.12.2 5-in-a-row: confetti burst + "5 STREAK!" flash banner (800ms)
- [ ] UX.12.3 10-in-a-row: multi-color confetti + "UNSTOPPABLE!" banner + bonus gems
- [ ] UX.12.4 Streak counter in HUD (appears after first correct tap, disappears on wrong)

#### UX.13 — Result Polish ⬜
- [ ] UX.13.1 "NEW PERSONAL BEST" badge on ΔE stat card (`isNewPersonalBest` already in state)
- [ ] UX.13.2 ΔE tier name on result (e.g. "EXPERT" badge alongside ΔE value)

#### UX.14 — Swatch Size Setting ⬜
*Infrastructure complete — expose to user.*
- [x] UX.14.0 `SwatchSize` enum (`Normal` 1.0× / `Medium` 1.2×); `ACTIVE_SWATCH_SIZE` constant in `RadialSwatchLayout.kt`; currently hardcoded to `Medium`
- [ ] UX.14.1 Wire `SwatchSize` to a user setting in SQLDelight `user_settings`
- [ ] UX.14.2 Add toggle in Settings screen (or game card options)

#### UX.15 — Settings / About Screen ⬜
- [ ] UX.15.1 **About screen** (or bottom sheet) — app version, legal links (Privacy Policy, Terms of Use), acknowledgements
- [ ] UX.15.2 **Health & Eye Strain notice** — persistent, always accessible from About. Balanced copy (see UX.5.3): *"This game exercises colour perception. Take breaks. Stop if you feel eye strain or discomfort."* No alarmist language (App Store / Play Store content guidelines require factual, non-fear-based health copy).
- [ ] UX.15.3 Privacy Policy link (required by both stores)

### Phase 7 — Monetization ⬜
- [ ] 7.1 Attempt counter on Threshold card ("X of 10 tries used this window")
- [ ] 7.2 Out of Tries refill sheet (see UX.11) — replaces blocked screen
- [ ] 7.3 `PaywallSheet` — "Unlock Forever — $2" primary CTA + "Watch Ad (+1 try)" ghost secondary
- [ ] 7.4 AdMob setup — rewarded ad for +1 try (Android + iOS)
- [ ] 7.5 IAP setup — one-time "Unlimited" product (Google Play Billing + StoreKit 2)
- [ ] 7.6 Persist `is_paid = true` in SQLDelight `user_settings` on purchase
- [ ] 7.7 If paid: hide ads, remove attempt cap, show "Unlimited" badge on Threshold card

### Phase 8 — Firebase Leaderboard ⬜
- [ ] 8.1 Firebase project setup, enable Realtime DB + Anonymous Auth
- [ ] 8.2 Firebase config — Android `google-services.json`, iOS `GoogleService-Info.plist`
- [ ] 8.3 Add `firebase-gitlive` KMP SDK to `libs.versions.toml`
- [ ] 8.4 Schema: `/leaderboard/{uid}: { name, deltaE, timestamp }`
- [ ] 8.5 Query: order by `deltaE` ascending, limit 50
- [ ] 8.6 `LeaderboardScreen` — ranked list, your entry highlighted
- [ ] 8.7 Submit flow: name input sheet → push to Firebase
- [ ] 8.8 Security rules: public read, anon-auth write, max 1 entry per UID
- [ ] 8.9 Player rank: query position after submission; wire into `HomeUiState.Ready.rank` (currently "—")
- [ ] 8.10 Streak tracking: consecutive daily completions; wire into `HomeUiState.Ready.streak` (currently 0)

### Phase 9 — Polish & Ship ⬜
- [ ] 9.1 App icon (all sizes) + splash screen
- [ ] 9.2 Verify on real Android device + iOS device/simulator
- [ ] 9.3 Haptic + sound tuning pass on real device
- [ ] 9.4 Play Store listing — screenshots, description, content rating
- [ ] 9.5 App Store listing — screenshots, description, review submission

### Phase T — Testing ⬜
*All reward logic is isolated for unit testing — no mocking of DB needed for core logic.*
- [ ] T.1 `GameRewardRates` — pure constants; verify gem rate values
- [ ] T.2 `PlayerLevel.fromGems()` — pure function; test all tier boundaries
- [ ] T.3 `ThresholdViewModel` — inject fake `ColorEngine`, `ThresholdRepository`, `SettingsRepository`; drive via `onUiEvent()`; verify `sessionGems`, milestone bonuses, try budget exhaustion
- [ ] T.4 `DailyViewModel` — inject fakes; verify correct/wrong round handling, participation gem, perfect bonus, 6-round-always rule
- [ ] T.5 `ColorMath` — `deltaE()` round-trip tests against known CIEDE2000 reference values

### Phase L — Font Licenses ⬜ (before App Store submission)
- [ ] L.1 **Clash Display (Fontshare FF EULA)** — verify bundling inside APK/IPA is acceptable under clause 01. If any concern: replace with **DM Serif Display** or **Syne** (both SIL OFL).
- [ ] L.2 **Bebas Neue (SIL OFL 1.1)** — no action needed.
- [ ] L.3 **Space Grotesk (SIL OFL 1.1)** — no action needed.
- [ ] L.4 Remove unused fonts (Antonio, Fredoka) from `composeResources/font/` before ship.
- [ ] L.5 Add font attribution to app's About/Settings screen if required by Clash Display license.

---

## Ship Checklist (pre-submission)

- [ ] All Phase 7 (monetization) items complete
- [ ] Phase 8 (Firebase) live or hidden behind feature flag
- [ ] DS.3 haptics + DS.4 sound wired
- [ ] Font license review complete (Phase L)
- [ ] 6.11 personal best badge on Result
- [ ] UX.5.1 first-launch onboarding
- [ ] UX.6.2 leaderboard button hidden until Firebase live
- [ ] App icon + splash done (9.1)
- [ ] Tested on real Android + iOS device (9.2)
