# Huezoo — Design System
*Single source of truth. Supersedes: TYPOGRAPHY.md, kinetic_neon/DESIGN.md, STITCH_DESIGN_REVIEW.md, IMPLEMENTATION_PLAN_LEVELS_ECONOMY_UX.md, ux.md.*
*Companion docs still active: `MVP.md` (implementation tracker) · `VISION.md` (product roadmap)*

---

## 1. Philosophy & North Star

> **"The Kinetic Vault"** — a high-end, tactile instrument where light behaves as a physical material.

This is not a settings screen. Not Material 3 defaults. Not a web dashboard.
Every tap should feel like something happened. Every correct answer should feel satisfying.
Every wrong answer should **sting** — politely, but deeply.

The visual language is **dark, vibrant, neo-brutalist, illustrated**.
- Sharp rectangular corners on panels (0dp radius) — architectural, aggressive
- Neon rim lighting defines edges, not traditional borders
- "Physical shelves" under buttons and cards — depth through offset shadows, not blur
- Canvas-drawn illustrations built from geometric lines — GI Joe / military-sensor aesthetic
- Every screen should feel like something a player is proud to screenshot

---

## 2. The Sting Principle — Core UX + Copy Rule

**This is the most important rule in the app. Never forget it.**

Wrong answers don't say "Oops! Try again :)" — that's a toddler app.
Wrong answers tease. They challenge. They make the player feel like they *almost* had it
and now they *have* to go again.

> Polite enough that it doesn't feel mean. Sharp enough that you can't just close the app.

**Emotion designed for:** Not shame. Not frustration. **Determination.**

### Rules for All In-App Copy

1. Never say sorry. The app never apologizes.
2. Never use exclamation marks on wrong answers. "Wrong!" feels aggressive. "That wasn't it." feels like a raised eyebrow.
3. Always leave a door open. Every loss message implies the next attempt is closer.
4. Reference their specific failure. "ΔE 2.4 got you" stings more than generic "You missed."
5. Tease the ceiling. Show them the gap between where they are and where they could be.
6. On wins, be cool not hype. Don't over-celebrate easy wins.

### Copy Tone by Moment

| Moment | Tone | Example |
|---|---|---|
| Game start | Confident dare | "Let's see what you've got." |
| Correct (easy) | Understated | "Not bad." |
| Correct (hard) | Genuine respect | "Okay. That one was real." |
| Wrong (early) | Light tease | "ΔE 3.1 already? Hmm." |
| Wrong (deep) | Real sting | "You were at ΔE 1.4. So close it hurts." |
| New personal best | Cool, not hype | "ΔE 1.1. That's actually impressive." |
| No improvement | Dare | "Same as last time. You sure that's your limit?" |
| Daily done | Matter-of-fact | "Day 47. Done. Come back tomorrow." |
| Out of tries | Wry | "5 tries. Still not satisfied? Respect." |

### Sting Copy Pool — Result Screen (by ΔE achieved)

```
ΔE < 0.5  → "Superhuman. Seriously."
ΔE < 1.0  → "Your eyes are elite."
ΔE < 1.5  → "Sharp. Very sharp."
ΔE < 2.0  → "Better than most."
ΔE < 3.0  → "Solid perception."
ΔE < 4.0  → "Room to grow."
ΔE ≥ 4.0  → "Keep training."
Daily >800 → "Perfect run."
Daily >500 → "Strong."
Daily >200 → "Not bad."
Daily else → "Try again tomorrow."
```

### Wrong Answer Copy Pool

**Early miss (ΔE ~3.0+):** "That one wasn't even close." / "ΔE 3.1. Your eyes were off today." / "Warmer. Not warm enough."

**Mid-game (ΔE ~1.5–3.0):** "ΔE {X} got you. Most people tap out here." / "That gap was real. You almost saw it."

**Deep miss (ΔE < 1.5):** "ΔE {X}. That's sub-pixel territory. Seriously impressive." / "ΔE {X}. The difference was barely real. You still missed it."

### Home Screen Copy

- App tagline: **"How sharp are your eyes?"**
- The Threshold subtitle: **"One miss. That's all it takes."**
- Daily Challenge subtitle: **"Everyone gets the same puzzle. Not everyone survives it."**

---

## 3. Color System

### Surfaces (static dark — never change with system theme)

```
Background    #080810   deep space void — screen root
SurfaceL0     #0D0D18   deepest panel inset
SurfaceL1     #12121E   card face, game board
SurfaceL2     #1C1C2E   elevated panels, sheets, inner card panels
SurfaceL3     #26263A   pressed states, stat box backgrounds
SurfaceL4     #34343F   shelf color (shadow offset target), separators
```

### Accents (neon — interactive elements, identity colors, glows)

```
AccentCyan     #00E5FF   primary CTA, "correct", Threshold active, Level 1
AccentMagenta  #FF2D78   wrong answer, danger, Level 2 / Skilled
AccentYellow   #FFE600   Daily Challenge identity, Level 3 / Master
AccentPurple   #9B5DE5   secondary purple accent
AccentGreen    #00F5A0   success, streak complete, ActionConfirm
WarmOrange     #FF8A50   warm accent
```

### Game Identity Colors

```
GameThreshold  #7B6FF0   indigo-violet — used in ThresholdHeroCard illustration + tints
GameDaily      #FFE600   AccentYellow — Daily Challenge
```

### Text (always on dark surfaces — use these, NOT MaterialTheme.colorScheme on cards)

```
TextPrimary    #FFFFFF   all primary text on dark cards/panels
TextSecondary  #9898BB   subtitles, metadata, secondary labels (~5.5:1 on SurfaceL1)
TextDisabled   #777799   personal best, helper labels, faded states (~4.5:1 on SurfaceL1)
```

### The Two-Surface Rule — Critical

Huezoo has two surface types:

| Surface | Background | Text tokens |
|---|---|---|
| **Static dark** (cards, buttons, sheets, game panels) | `HuezooColors.SurfaceL1/L2` — NEVER changes with theme | `HuezooColors.TextPrimary / TextSecondary / TextDisabled` — always explicit |
| **App page background** (screen root) | `MaterialTheme.colorScheme.background` | `MaterialTheme.colorScheme.onBackground` — defaults OK |

`MaterialTheme.colorScheme.onBackground` is near-black in light mode. Cards are always dark.
Near-black on dark card = invisible. Always use explicit `HuezooColors.Text*` on cards.

### Contrast & Color Safety (`ColorExt.kt`)

```kotlin
// Always use on colored backgrounds (buttons, badges, identity chips)
val textColor = faceColor.onColor          // → #0D0D1A (dark) or #FFFFFF (white)

// Explicit contrast check
val ratio = HuezooColors.AccentCyan.contrastRatio(HuezooColors.Background) // ~12.9

// With preferred color
val label = HuezooColors.AccentCyan.foregroundColor(preferred = Color.White) // → #0D0D1A (fails)
```

AccentPurple is the only accent that gets white text — all others get near-black text via `onColor`.

---

## 4. Typography

### Font Families — Three Fonts, Strict Roles

| Font | Role | Used for |
|---|---|---|
| **Bebas Neue** | Bold condensed display (replaces Antonio) | Numbers, scores, ΔE values, app name, hero text |
| **Clash Display** | Round, architectural (replaces Fredoka) | Card titles, section headings, paywall title |
| **Space Grotesk** | Geometric sans-serif | Body copy, button labels, metadata, badges, all small UI |

Never use `Text()` directly. Always use typed `HuezooText` composables from `HuezooText.kt`.

### Full Type Scale

| Slot | Composable | Font | Weight | Size | Use |
|---|---|---|---|---|---|
| `displayLarge` | `HuezooDisplayLarge` | Bebas Neue | Bold | 56sp | ΔE hero on ResultCard |
| `displayMedium` | `HuezooDisplayMedium` | Bebas Neue | Bold | 40sp | Gems count, SCORE stat |
| `displaySmall` | `HuezooDisplaySmall` | Bebas Neue | Bold | 28sp | DeltaEBadge number |
| `headlineLarge` | `HuezooHeadlineLarge` | Bebas Neue | Bold | 40sp | App name, screen hero |
| `headlineMedium` | `HuezooHeadlineMedium` | Clash Display | SemiBold | 28sp | Section headings, dialog titles |
| `headlineSmall` | `HuezooHeadlineSmall` | Bebas Neue | Medium | 20sp | Currency pill, inline numbers |
| `titleLarge` | `HuezooTitleLarge` | Clash Display | Bold | 22sp | Card headers "THE THRESHOLD" |
| `titleMedium` | `HuezooTitleMedium` | Clash Display | SemiBold | 20sp | GameCard title |
| `titleSmall` | `HuezooTitleSmall` | Clash Display | Regular | 16sp | Secondary titles |
| `bodyLarge` | `HuezooBodyLarge` | Space Grotesk | Regular | 18sp | Long-form description |
| `bodyMedium` | `HuezooBodyMedium` | Space Grotesk | Regular | 16sp | Card subtitle, description |
| `bodySmall` | `HuezooBodySmall` | Space Grotesk | Regular | 14sp | Captions, hints |
| `labelLarge` | `HuezooLabelLarge` | Space Grotesk | Bold | 16sp | Button labels, chips |
| `labelMedium` | `HuezooLabelMedium` | Space Grotesk | Medium | 13sp | Secondary labels |
| `labelSmall` | `HuezooLabelSmall` | Space Grotesk | Medium | 12sp | Badges, stat headers, tries text |

### Font Licenses

- **Bebas Neue** — SIL OFL 1.1 (no action needed)
- **Clash Display** — Fontshare FF EULA — verify bundling in APK/IPA is acceptable before App Store submission; fallback: DM Serif Display (SIL OFL)
- **Space Grotesk** — SIL OFL 1.1 (no action needed)

---

## 5. Visual Language — Neo-Brutalist Kinetic

### The Physical Shelf

Depth is achieved by offset hard-edge shadows — not blur, not soft glow.
Every elevated element sits on a "shelf": a darker layer offset (+4dp, +4dp) bottom-right.

```kotlin
// Applied via Modifier.shapedShadow() from Modifiers.kt
modifier.shapedShadow(
    shape = RectangleShape,
    color = HuezooColors.SurfaceL4,
    offsetX = 4.dp,
    offsetY = 4.dp,
)
```

### Neon Rim Light — No-Line Rule

**Prohibit 1px solid borders.** Edges are defined by light. Use `rimLight()` modifier to simulate a physical light source hitting the top-left corner of a surface:

```kotlin
modifier.rimLight(cornerRadius = 0.dp)
// Draws a faint inset highlight on top + left edges in AccentCyan at ~0.30 alpha
```

### AmbientGlowBackground

Every screen root uses two large radial gradients anchored at opposite corners — top-start (primary) and bottom-end (secondary) — at 10% alpha. This creates ambient identity color bleed without affecting color accuracy in game panels.

```kotlin
AmbientGlowBackground(
    primaryColor = HuezooColors.AccentCyan,   // default
    secondaryColor = HuezooColors.AccentMagenta,
) { content() }
```

### Edge-to-Edge

`enableEdgeToEdge()` is called in `MainActivity`. `AmbientGlowBackground` fills under the status bar.
Status bar padding is applied via `WindowInsets.statusBars` inside `HuezooTopBar`.
**On the Home screen, `HuezooTopBar` is inside the scrollable Column** — it scrolls with content (not pinned), and its `windowInsetsTopHeight` spacer handles the status bar gap.

---

## 6. Modifier Toolkit

All custom modifiers live in `ui/theme/Modifiers.kt`.

### `shapedShadow(shape, color, offsetX, offsetY)`
Neo-brutal offset shadow. Draws a filled shape at `(+offsetX, +offsetY)` behind the component.
Works with any `Shape`; `RectangleShape` uses the fast `Outline.Rectangle` branch.
Always apply **before** `.background()` in the modifier chain.

### `rimLight(cornerRadius)`
Top + left inset highlight in `AccentCyan.copy(0.30f)`.
Simulates a physical light source at the top-left corner of a card.
Apply as the **last** modifier after `.background()`.

### Usage order on a typical card

```kotlin
modifier
    .shapedShadow(RectangleShape, HuezooColors.SurfaceL4, 4.dp, 4.dp) // shadow behind
    .background(HuezooColors.SurfaceL2)                                 // fill
    .rimLight(cornerRadius = 0.dp)                                       // light on top
```

---

## 7. Shapes

```
ParallelogramBack     Custom GenericShape   Back button (skewed left)
RectangleShape        System               All panel cards, hero cards, stat boxes
RoundedCornerShape    System (per use)     DeltaEInfoCard (CornerCard token)
```

`HuezooSize.CornerCard` = the standard corner radius for any card that uses rounded corners (e.g. `DeltaEInfoCard`). All hero cards / panel cards use `RectangleShape` (0dp radius — architectural sharp).

`HuezooSpacing.*` provides the spacing scale: `xs, sm, md, lg, xl`.

---

## 8. Components

All in `composeApp/src/commonMain/kotlin/xyz/ksharma/huezoo/ui/components/`.

### HuezooTopBar
Glassmorphic bar. Vertical frost gradient Background 95%→82%. 4dp bottom border in `SurfaceL1`.
- Home/root screen: shows wordmark `HUEZ` (Bebas Neue italic, AccentCyan) — no gems
- Inner screens: shows parallelogram back button (cyan shadow, `SurfaceL3` face)
- `windowInsetsTopHeight(WindowInsets.statusBars)` spacer handles status bar
- Scrolls with content on Home screen (not fixed)

### HuezooButton
Pill-shaped. Variants: `Primary` (Cyan), `Confirm` (Green), `Danger` (Magenta), `Score` (Yellow), `Ghost` (transparent + cyan border), `GhostDanger` (transparent + magenta border).
Text: `HuezooLabelLarge`, color via `.onColor`. No X shadow — bottom-only shelf.

### HuezooIconButton
Squircle 48×48dp, 4dp bottom shelf. Variants: Dismiss (red), Confirm (green), Back (dark), Info (cyan). Use instead of any `TopAppBar` back arrow in game screens.

### PriceButton
Wide pill, 56dp tall. Bright green face. Used only for purchase CTAs.

### SwatchBlock
Color swatch with states: default / pressed / correct / wrong / revealed.
Shake animation (ShakeX) on wrong. Scale spring on appear.

### RadialSwatchLayout
Flower-petal radial arrangement of swatches (6 petals). Used in both Threshold and Daily games.

### SwatchGradientOverlay
Top-left highlight + bottom-right shadow over a swatch for physical chip look.

### SkewedStatChip
Parallelogram-shaped HUD chip (skewed). Shows game stats (ROUND, TRIES). Add ~3dp `paddingStart` offset to clear the angled boundary on the left edge.

### DeltaEBadge
Animated ΔE readout. Color shifts: `AccentCyan` (easy, >3.0) → `AccentYellow` (medium, 1.5–3.0) → `AccentMagenta` (hard, <1.5). Bebas Neue Bold for the number.

### RoundIndicator
Row of dots: inactive / active (pulse + scale 1.2f + white border) / completed (accent green).

### ResultCard
Share-ready end-of-game card. Spring entrance (slide up 60dp + scale 0.9→1.0). Count-up animation for score and ΔE. Sting copy based on ΔE achieved.

### GameCard
Legacy card component. Hero illustration area (90dp). Identity color accent bar. Personal best. Still used in some contexts; Home screen now uses custom card components instead.

### CurrencyPill
Display-only pill. SurfaceL2 background. No press animation. Not shown on Home screen TopBar — gems are in the Home Stats Section only.

### AmbientGlowBackground
Screen-level background. Two radial gradients at 10% alpha anchored at opposite corners. Never intercepts touch events. See §5.

### HuezooBottomSheet
Game-styled modal sheet. 32dp top corners, handle bar. Used for paywall.

---

## 9. Illustration System

**Rule:** Every major game card should have a canvas-drawn illustration that communicates its personality through geometry and lines — not photographs or generic gradients.

### Philosophy

Illustrations are drawn entirely from **lines, arcs, dots, and geometric shapes** in the game's identity color at low opacity. They sit behind content as a full-card background layer. The visual metaphor matches the game's theme:
- **Threshold** → Tactical scanner / targeting computer (precision, military sensor)
- **Daily** → Pending design (see below)

### How to Apply

```kotlin
Box(modifier = cardModifier) {
    // Illustration fills the full card behind content
    MyGameIllustration(
        enabled = enabled,
        modifier = Modifier.matchParentSize(),
    )
    // Content column on top
    Column(modifier = Modifier.fillMaxWidth().padding(...)) { ... }
}
```

The illustration composable wraps a `Canvas(modifier = modifier)` that draws into the full card bounds.
Canvas clips automatically to its bounds — elements drawn beyond `size.width` are cut off cleanly.

### Threshold Scanner Illustration (`ThresholdScannerIllustration`)

Built from these layers, all in `HuezooColors.GameThreshold` + `AccentCyan` accents:

| Layer | What it draws | Alpha |
|---|---|---|
| Dot grid | Evenly spaced dots, fades left (protecting text readability) | 0.07 |
| Range arcs (×5) | Concentric partial arcs from right edge (~190° sweep) | 0.22 → 0.10 |
| Arc tick marks | Major (every 4th) + minor ticks on each arc | 0.25 → 0.12 |
| Crosshair | H + V lines through reticle center, gapped around focal point | 0.13 |
| Sweep arm | Primary arm + ghost trailing arm 14° behind | 0.30 / 0.10 |
| Center ornament | Diamond (stroked, not filled) + AccentCyan dot | 0.45 / 0.80 |
| Data blips | Small squares at specific arc/angle positions; accent blips in cyan | 0.35–0.60 |
| Corner brackets | Military L-shaped markers at all 4 corners | 0.22 |
| Data scan line | Horizontal line at 60% height + tick marks | 0.08–0.12 |

All alphas multiply by `dimFactor` (1.0 enabled, 0.35 when card is blocked/disabled).

### Adding a New Illustration

1. Create `private fun DrawScope.draw[Name]Illustration()` or a `@Composable fun [Name]Illustration(modifier)`.
2. Pick a visual metaphor that matches the game's personality.
3. Draw only with strokes and geometric fills — no raster images.
4. Anchor focal elements to one edge (usually right) so they bleed off screen slightly.
5. Fade elements on the left third of the card (use alpha gradient logic on x-position) to keep text legible.
6. Accept `enabled: Boolean` and multiply all alphas by `dimFactor = if (enabled) 1f else 0.35f`.
7. Use the game's identity color as the primary drawing color + `AccentCyan` for focal accents.

### Canvas Icon Drawers

For compact card icons (36dp size), use `DrawScope` extension functions typed as `DrawScope.(Color) -> Unit`:

```kotlin
// ✅ Correct — DrawScope extension, called inside Canvas {}
private fun DrawScope.drawMedalStar(color: Color) { ... }
Canvas(Modifier.size(36.dp)) { drawMedalStar(accentColor) }

// ❌ Wrong — @Composable lambda cannot be called inside Canvas {}
val icon: @Composable (Color) -> Unit = { c -> Icon(...) } // won't compile
```

Existing canvas icons: `drawMedalStar` (5-pointed star), `drawLeaderboardBars` (ascending bars).

---

## 10. Screen Designs

### Home Screen

Scrollable, edge-to-edge. TopBar scrolls with content (not pinned).

```
HuezooTopBar               ← wordmark, scrolls away with content
──────────────────────────
CURRENT INVENTORY          ← HuezooLabelSmall, TextDisabled
┌─ Cyan bar ─────────────┐
│  1,250  GEMS            │  ← shapedShadow, IntrinsicSize.Min Row
└────────────────────────┘
┌──────────┐  ┌──────────┐
│ STREAK   │  │  RANK    │  ← StatBox, equal weight(1f)
│ 0 DAYS   │  │  —       │
└──────────┘  └──────────┘

┌──────────────────────────┐
│ ThresholdScannerIllus... │  ← matchParentSize Canvas behind content
│ • ACTIVE MISSION         │
│ THE THRESHOLD            │
│ Description...           │
│ 5 / 5 TRIES REMAINING    │
│ [ENTER SIMULATION]       │  ← only this button navigates, not the card
│ ████░░ ROOKIE  SKILLED   │  ← LevelProgressBar
└──────────────────────────┘

┌──────────────────────────┐
│ ★  DAILY CHALLENGE       │  ← AccentMagenta, fullWidth
│    ULTRAVIOLET BURST     │
│    Available now         │
└──────────────────────────┘

┌──────────────────────────┐
│ ▐▌ GLOBAL LEADERBOARD    │  ← AccentYellow, fullWidth, disabled
│    TOP 5% WORLDWIDE      │
│    Claim weekly rewards  │
└──────────────────────────┘

▼ WHAT IS ΔE? ▲            ← DeltaEInfoCard, expand/collapse, always visible
```

**Key rules:**
- Gems shown in StatsSection only — NOT in TopBar
- Threshold hero card is NOT tappable — only the "ENTER SIMULATION" button navigates
- Compact cards (Daily, Leaderboard) are full-width, stacked in a Column (not Row)
- Challenge name is date-seeded from `CHALLENGE_NAMES[dayOfYear % size]`
- All cards stagger-animate in (80ms delay per index)

### The Threshold Game Screen

Skewed stat chips (ROUND + TRIES). Radial swatch layout. Fixed-height feedback slot (graphicsLayer alpha — no layout shift). DeltaEBadge (color by difficulty tier). ΔE correct-tap label animates in/out.

### Daily Challenge Game Screen

Same layout as Threshold. Shows today's date subtitle ("March 20 · Same for everyone"). Final round (6/6): feedback slot shows "Last one — make it count" on tap.

### Result Screen

Slide-up ResultCard entrance (offset 60dp + scale 0.9→1.0, spring). Count-up for ΔE and score. Confetti burst (identity color particles). Sting copy by ΔE. Share button → native sheet. Play Again button checks attempt gate (shows "NO TRIES LEFT" + `GhostDanger` variant when `!canPlayAgain`).

### Already-Played / Blocked Screens

Daily already-played: "ALREADY PLAYED" styled header + score card read-only + live countdown to next puzzle + "BACK TO HOME" primary button.

Threshold blocked: live "Resets in Xh Xm" countdown + "Back to Home" primary button + personal best as consolation.

---

## 11. Game Identity Colors

| Game | Color token | Hex | Level |
|---|---|---|---|
| The Threshold | `GameThreshold` | `#7B6FF0` | — |
| Daily Challenge | `AccentYellow` | `#FFE600` | Level 3 / Master |
| Level 1 / Rookie | `AccentCyan` | `#00E5FF` | Cyan |
| Level 2 / Skilled | `AccentMagenta` | `#FF2D78` | Magenta |
| Level 3 / Master | `AccentYellow` | `#FFE600` | Gold |

Identity color is used as: card accent bar, card background tint, ΔE badge color, result card glow, `rimLight` tint, `AmbientGlowBackground` primary color.

---

## 12. Levels, Economy & Progression

### Player Level (`PlayerLevel.kt`)

```kotlin
enum class PlayerLevel(val displayName: String, val minGems: Int, val levelColor: Color) {
    Rookie(displayName = "ROOKIE",  minGems = 0,       levelColor = HuezooColors.AccentCyan),
    Skilled(displayName = "SKILLED", minGems = 400,     levelColor = HuezooColors.AccentMagenta),
    Master(displayName = "MASTER",  minGems = 100_000, levelColor = HuezooColors.AccentYellow);
    companion object {
        fun fromGems(gems: Int): PlayerLevel = when {
            gems >= 100_000 -> Master; gems >= 400 -> Skilled; else -> Rookie
        }
    }
}
```

Level progress bar shown at bottom of Threshold hero card. Streak and Rank shown as stat boxes on Home screen (stubbed at 0 / null until Firebase wired — Phase 8.9 / 8.10).

### Gem Economy (current implementation)

- **Earning**: gems accumulated via `SettingsRepository.addGems()`; earn rate defined per gameplay session
- **Display**: `HomeUiState.Ready.totalGems` → formatted as `1,250` via `formatGems()`
- **Spending**: Threshold refill (300 gems → 10 questions) — not yet wired, Phase 7

### Attempt Window (Threshold)

5 tries per 8-hour window. `next_reset_at` stored as ISO timestamp in SQLDelight.
`ThresholdRepository.getAttemptStatus(now)` returns `AttemptStatus.Available` or `AttemptStatus.Exhausted(nextResetAt)`.

### ΔE Tier Labels (reference — not yet wired into HUD)

| ΔE range | Label | Meaning |
|---|---|---|
| 5.0–10.0 | BEGINNER | Easy — obvious difference |
| 3.0–5.0 | TRAINING | Visible but not trivial |
| 2.0–3.0 | SHARP | Requires focus |
| 1.0–2.0 | EXPERT | Hard — expert territory |
| 0.5–1.0 | ELITE | Very hard — professional colorist level |
| < 0.5 | SUPERHUMAN | Near limits of human vision |

---

## 13. UX Rules & Moments

### Missing UX Moments (status)

| Moment | Status | Target |
|---|---|---|
| New user opens app | ✅ DeltaEInfoCard always visible, expand/collapse | — |
| Correct tap | ✅ "↓ ΔE X.X — SHARPER" label 700ms | — |
| Wrong tap | ✅ Shake + reveal, 450ms delay | — |
| Result with great score | ✅ Confetti + sting copy | — |
| Daily complete, home | ✅ "Done" + score + countdown | — |
| Threshold blocked, home | ✅ "No tries" + reset countdown | — |
| Share score | ✅ Share button → native sheet | — |
| Play Again when tries exhausted | ✅ "NO TRIES LEFT" GhostDanger | — |
| Streak tracking | ⬜ Wired to 0 placeholder | Phase 8.10 |
| Player rank | ⬜ Wired to null placeholder | Phase 8.9 |
| Confetti threshold (only good scores) | ⬜ | UX.11 |

### ΔE Info Card (DeltaEInfoCard)

Always visible on Home screen. Expand/collapse with animated chevron. Local `remember` state (not persisted). Never dismissable. Always the last item before debug reset.

### Navigation Rules

- Home → Threshold game: `onNavigate(ThresholdGame)`
- Home → Daily game: `onNavigate(DailyGame)`
- Game → Result: carries `GameId`, `deltaE`, `score`, `gameType`
- Result → Home: pop back stack
- Result → Play Again: only if attempts available (checked in `ResultViewModel`)
- Leaderboard button: hidden until Firebase integrated

---

## 14. Non-Negotiables

These must survive every future iteration:

1. `HUEZ` wordmark in Bebas Neue italic — every screen
2. 3 levels: Rookie / Skilled / Master (gem thresholds: 0 / 400 / 100,000)
3. Level colors: Cyan (L1) / Magenta (L2) / Gold (L3)
4. Fixed-height feedback slot during gameplay — no layout shift
5. Economy: gems earned in gameplay, 300 gems = 10 tries refill
6. No forced ads mid-round
7. TopBar is NOT a Material3 `TopAppBar` — use `HuezooTopBar` only
8. No bare `Text()` composable — always `HuezooText` variants
9. No hardcoded `Color.White` on colored backgrounds — use `.onColor`
10. No light theme — Huezoo is permanently dark

---

## 15. What Never to Do

```
❌ NEVER use TopAppBar with back arrow in game screens
✅ Use HuezooIconButton(variant = Back / Dismiss) instead

❌ NEVER use diagonal/blurry drop shadow
✅ shapedShadow() — hard-edge offset, bottom-right only

❌ NEVER use emoji as UI elements
✅ Vector composables from composeResources/drawable/

❌ NEVER use bare Text() anywhere in screens or components
✅ HuezooText variants from HuezooText.kt

❌ NEVER use MaterialTheme.colorScheme.onBackground on card/button surfaces
✅ HuezooColors.TextPrimary / TextSecondary / TextDisabled explicitly

❌ NEVER hardcode Color.White or Color.Black for text on colored backgrounds
✅ .onColor from ColorExt.kt

❌ NEVER use light theme — Huezoo is dark-only permanently

❌ NEVER put gems in the TopBar on Home screen
✅ Gems live in the StatsSection below the TopBar only

❌ NEVER make the entire Threshold hero card tappable
✅ Only the "ENTER SIMULATION" button navigates

❌ NEVER use @Composable lambdas as Canvas icon content
✅ DrawScope.(Color) -> Unit extension functions, called inside Canvas {}

❌ NEVER blur values over 5px
✅ Hard-edge depth (shelf + rimLight) — no soft glows
```

---

## 16. File Reference

```
composeApp/src/commonMain/
├── composeResources/
│   ├── drawable/          ← all vectors (ic_close, ic_check, game art, etc.)
│   └── font/              ← bebas_neue_regular, clash_display_*, space_grotesk_* .ttf
└── kotlin/xyz/ksharma/huezoo/
    ├── App.kt                          ← nav host, screen wiring
    ├── navigation/                     ← route objects (ThresholdGame, DailyGame, Result…)
    ├── domain/
    │   ├── color/
    │   │   ├── ColorMath.kt            ← rgbToLab, CIEDE2000 deltaE
    │   │   ├── ColorEngine.kt          ← randomVividColor, generateOddSwatch, scoreFromDeltaE
    │   │   └── Lab.kt                  ← Lab data class + Lab.toColor()
    │   └── game/
    │       ├── ThresholdGameEngine.kt  ← game loop logic
    │       ├── DefaultDailyGameEngine.kt
    │       └── model/AttemptStatus.kt
    ├── data/
    │   ├── db/                         ← SQLDelight schema + generated queries
    │   └── repository/
    │       ├── ThresholdRepository.kt  ← getAttemptStatus, getPersonalBest
    │       ├── DailyRepository.kt      ← getChallenge, markCompleted
    │       └── SettingsRepository.kt   ← isPaid, getGems, addGems, resetAll
    ├── platform/
    │   └── PlatformOps.kt              ← shareText, isDebugBuild (expect/actual)
    └── ui/
        ├── theme/
        │   ├── Color.kt                ← HuezooColors token object
        │   ├── ColorExt.kt             ← contrastRatio, foregroundColor, onColor
        │   ├── Typography.kt           ← BebasNeue/ClashDisplay/SpaceGrotesk + scale
        │   ├── Shape.kt                ← ParallelogramBack + HuezooSize/HuezooSpacing tokens
        │   ├── Modifiers.kt            ← shapedShadow, rimLight
        │   ├── Dimensions.kt           ← HuezooSpacing, HuezooSize dimension tokens
        │   └── Theme.kt                ← HuezooTheme (dark-only)
        ├── components/
        │   ├── HuezooText.kt           ← ALL typed text composables — only file using Text()
        │   ├── HuezooButton.kt         ← pill + shelf, Primary/Confirm/Danger/Ghost/GhostDanger
        │   ├── HuezooIconButton.kt     ← squircle icon buttons (X, ✓, back, info)
        │   ├── HuezooTopBar.kt         ← wordmark + back button + status bar spacer
        │   ├── PriceButton.kt          ← purchase CTA
        │   ├── AmbientGlow.kt          ← screen-level background glow
        │   ├── GameCard.kt             ← legacy card (frame + inner panel + shelf)
        │   ├── ResultCard.kt           ← end-of-game share card
        │   ├── SwatchBlock.kt          ← color swatch with states + shake animation
        │   ├── RadialSwatchLayout.kt   ← 6-petal flower arrangement
        │   ├── SwatchGradientOverlay.kt← top-left highlight chip effect
        │   ├── SkewedStatChip.kt       ← parallelogram HUD chip
        │   ├── DeltaEBadge.kt          ← animated ΔE display (color by difficulty)
        │   ├── RoundIndicator.kt       ← round progress dots
        │   ├── CurrencyPill.kt         ← gem + amount display
        │   └── HuezooBottomSheet.kt    ← game-styled paywall sheet
        ├── model/
        │   ├── PlayerLevel.kt          ← Rookie/Skilled/Master enum + fromGems()
        │   ├── RoundPhase.kt           ← game round phase enum
        │   ├── SwatchLayoutStyle.kt    ← layout style enum
        │   └── SwatchUiModel.kt        ← swatch UI model
        ├── home/
        │   ├── HomeScreen.kt           ← full redesign (scanner illustration, scrolling TopBar)
        │   ├── HomeViewModel.kt
        │   └── state/HomeUiState.kt / HomeUiEvent.kt
        ├── games/
        │   ├── threshold/
        │   │   ├── ThresholdScreen.kt
        │   │   ├── ThresholdViewModel.kt
        │   │   └── state/ThresholdUiState.kt / ThresholdUiEvent.kt / ThresholdNavEvent.kt
        │   └── daily/
        │       ├── DailyScreen.kt
        │       ├── DailyViewModel.kt
        │       └── state/DailyUiState.kt / DailyUiEvent.kt / DailyNavEvent.kt
        ├── result/
        │   ├── ResultScreen.kt         ← confetti, sting copy, canPlayAgain gate
        │   ├── ResultViewModel.kt
        │   └── state/ResultUiState.kt
        ├── leaderboard/
        │   └── LeaderboardScreen.kt    ← stub, pending Firebase (Phase 8)
        └── paywall/
            └── PaywallSheet.kt
```
