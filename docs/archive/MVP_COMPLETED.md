# Huezoo — Completed MVP Phases (Archive)

*Moved from MVP.md once all tasks in a phase are done. Source of truth for what shipped.*

---

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

### Phase DS.Font — Custom Typography ✅ (ship items pending)
- [x] DS.Font.1 **Bebas Neue** (Regular) — display/numbers font. SIL OFL.
- [x] DS.Font.2 **Clash Display** (Regular, Medium, SemiBold, Bold) — title/heading font. Fontshare FF EULA.
- [x] DS.Font.3 **Space Grotesk** (Regular, Medium, SemiBold, Bold) — body/label font. SIL OFL.
- [x] DS.Font.4 All `.ttf` files placed in `composeApp/src/commonMain/composeResources/font/`
- [x] DS.Font.5 `Typography.kt` updated — full type scale wired

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

### Phase DS.3 — Haptics ✅
- [x] DS.3.1 `HapticType` enum in commonMain (Selection, ButtonTap, GemEarned, CorrectTap, WrongTap, MilestoneHit, PerfectRun, GameOver)
- [x] DS.3.2 `HapticEngine` interface in commonMain
- [x] DS.3.3 Android actual — `VibrationEffect` patterns per type (API 26+, predefined on API 29+)
- [x] DS.3.4 iOS actual — `UIImpactFeedbackGenerator` / `UINotificationFeedbackGenerator`
- [x] DS.3.5 Koin binding (Android: `androidModule`, iOS: `IosModule`)
- [x] DS.3.6 Wired into gameplay: CorrectTap, WrongTap, MilestoneHit, GameOver, PerfectRun in both ViewModels; ButtonTap in `HuezooButton`; GemEarned in `CurrencyPill`

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
- [x] 2.1 `RadialSwatchLayout` wired to tap events and game state
- [x] 2.2 Result screen hero gems count-up + gem breakdown card
- [x] 2.3 Home screen game cards reading real data from SQLDelight
- [x] 2.4 `SkewedStatChip` live updates during game
- [x] 2.5 Round indicators on Daily card

### Phase 3 — Home Screen ✅
- [x] 3.1–3.7 All Home screen tasks complete (cards, gem panel, stats, level badge, lifecycle refresh)

### Phase 4 — The Threshold ✅
- [x] 4.1–4.11 All Threshold game loop tasks complete

### Phase 5 — Daily Challenge ✅
- [x] 5.1–5.6 All Daily game loop tasks complete

### Phase 6 — Result Screen ✅
- [x] 6.1–6.11 All Result screen tasks complete

### UX.10 — Progression Screens ✅
- [x] UX.10.1–10.3 Home, LevelsProgressSheet, SplashScreen

### UX.16 — Level-Driven UI Theming ✅
- [x] UX.16.1–16.10 `LocalPlayerAccentColor` wired across all 12 files

### UX.17 — Local Leaderboard (Pre-Firebase) ✅
- [x] UX.17.1–17.5 `PerceptionTier`, `LeaderboardViewModel`, `LeaderboardScreen`, `LeaderboardCompactCard`, paid threshold no-cooldown

### UX.18 — Perception Tiers Sheet ✅
- [x] UX.18.1–18.2 `PerceptionTiersSheet`, `PlayerDeltaECard` tap handler

### UX.19 — Personal Best Integrity ✅
- [x] UX.19.1–19.2 Drop-out save + wrong-tap best candidate

### UX.21 — 3D Shelf-Press (partial) ✅
- [x] UX.21.1 `HuezooButton` two-layer face/shelf architecture
- [x] UX.21.2 `UpgradeCta` shelf-press migration
- [x] UX.21.3 Ghost + GhostDanger inherit shelf-press fix
- [x] UX.21.4 `ShareIconButton` shelf-press migration

### Phase 7 (partial) ✅
- [x] 7.3 `UpgradeScreen` — dedicated paywall screen

### Phase 8 (partial) ✅
- [x] 8.10 Streak tracking: `getStreak()` in `DefaultDailyRepository`, wired to `HomeUiState`
