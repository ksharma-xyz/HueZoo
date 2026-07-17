# Handoff: Color Memory Match (Game 6)

> A new mini-game for Huezoo. Color A flashes for ~3 seconds, a memory chamber seals, Color B is revealed. The player taps **SAME** or **DIFFERENT**. 10 rounds, ΔE tightens each round, max score 100.

This is a planned Game 6 from `docs/VISION.md`. The MVP is two visual variations to choose between, plus the full state machine, scoring, and result-screen wiring.

---

## About the Design Files

The files in `prototype/` are **design references** built in HTML/React. They are not production code to copy or transpile. The task is to **recreate these designs in the existing Huezoo Compose Multiplatform codebase** (`composeApp/src/commonMain/kotlin/xyz/ksharma/huezoo/`) using its established Compose UI patterns, `HuezooColors` tokens, `HuezooText` type scale, and existing reusable components like `SkewedStatChip`, `RoundIndicator`, `HuezooButton`, `ResultCard`.

The HTML prototype mirrors the look and behavior pixel-for-pixel at the design-system level. Open `Color Memory Match.html` to interact with it. Use the floating **Tweaks** panel (bottom-right) to:

- Switch between **Variation A — The Vault** and **Variation B — Twin Lock**
- Change ΔE difficulty (`Easy`, `Medium`, `Hard`, `Elite`)
- Jump to any specific phase for review: Memory / Hold / Recall / Correct / Wrong / Result
- Force the next answer to be `SAME` or `DIFFERENT`
- Restart the session

---

## Fidelity

**High-fidelity.** Final colors, typography, spacing, copy, state transitions, scoring formula, gem rewards, and sting-copy pool are all spelled out below. Engineering should match pixel-for-pixel within the existing `HuezooColors` / `HuezooText` / `HuezooSpacing` token system.

---

## Pick a Variation Before Building

The proposal includes **two layout directions**. Pick one in design review before implementation.

| | **A · The Vault** | **B · Twin Lock** |
|---|---|---|
| Memory phase | Single 220dp central swatch ringed by a 24-segment countdown arc draining over 3 s | Two adjacent vertical chambers; Chamber A goes LIVE with the color, Chamber B is locked + striped + shows "?" |
| Seal phase | Swatch swaps for a diagonal-stripe SEALED chamber, blinking yellow "◉ SEALED" tag | A shutter (vertical scanlines, yellow rim) slides down inside Chamber A over 400 ms |
| Recall phase | Color B pops into the central position (magenta-rimmed scaffold) | Chamber B unlocks: "?" dissolves, Color B fades in; SAME / DIFFERENT enable |
| Feedback | A and B render side-by-side at smaller scale with `=` or `≠` glyph + TRUTH label | Chamber A shutter slides back up to reveal the original — direct A vs B comparison ("answer key" reveal) |
| Personality | Tactical / scanner — matches `ThresholdScannerIllustration` vocabulary | Forensic / diagnostic — locked memory + reveal as evidence |

Recommendation: **Variation A** if you want maximum visual consistency with The Threshold. **Variation B** if you want Color Memory Match to feel like its own thing — the comparison reveal is genuinely satisfying.

---

## Game Rules

(from `docs/VISION.md` Game 6, with this proposal's refinements baked in.)

- **Rounds**: 10 fixed
- **Memory duration**: 3000 ms (Color A visible)
- **Hold interstitial**: 400 ms (sealed/blank)
- **Recall**: until the player taps SAME or DIFFERENT (no timeout in MVP)
- **Feedback duration**: 1700 ms before auto-advance
- **Scoring**: `+10` correct, `−5` wrong, score may go negative
- **Max score**: 100
- **ΔE curve** (round 1 → 10): `5.0, 4.0, 3.0, 2.5, 2.0, 1.5, 1.2, 1.0, 0.7, 0.5`
- **`isSame` probability**: 50/50 per round (`Random.nextBoolean()`, daily-seedable later)
- **Identity color**: `HuezooColors.AccentPurple` (`#9B5DE5`) — distinct from Threshold's `GameThreshold`

### Gem economy

Slot into the existing pattern in `GameRewardRates.kt`:

| Event | Gems |
|---|---|
| Each correct round | +2 |
| Streak of 3 correct | +5 bonus (one-shot per streak) |
| Streak of 5 correct | +10 bonus (one-shot per streak) |
| Perfect run (10/10) | +25 bonus |

Adjust to taste in code review — but keep the per-round amount lower than Daily's `+5` because Memory Match runs are faster/cheaper than Daily.

### Score → tier label (for ResultCard header)

| Score | Tier label | Tone |
|---|---|---|
| ≥ 95 | `PERFECT EYE` | green identity |
| 80–94 | `STRONG RECALL` | identity color |
| 60–79 | `SOLID` | identity color |
| 40–59 | `ROOM TO GROW` | text-secondary |
| 10–39 | `EYES DRIFTED` | warm orange |
| < 10 | `FLATLINED` | magenta |

---

## State Machine

```kotlin
enum class CMMatchPhase {
  Memory,    // Color A visible, countdown active
  Hold,      // Brief blank/sealed interstitial (400 ms)
  Recall,    // Color B visible, SAME/DIFFERENT enabled
  Feedback,  // Reveal correct answer + sting copy (1700 ms)
  Result,    // Session complete
}
```

Transitions:

```
[Memory]  --3000ms--> [Hold]
[Hold]    --400ms-->  [Recall]
[Recall]  --player tap--> [Feedback]
[Feedback] --1700ms-->
    if round < 10  → [Memory] (round += 1)
    if round == 10 → [Result]
```

Suggested ViewModel API:

```kotlin
sealed class CMMatchUiState {
  object Loading : CMMatchUiState()
  data class Playing(
    val round: Int,           // 1..10
    val phase: CMMatchPhase,
    val score: Int,
    val results: List<RoundResult>, // size = round-1 (completed rounds)
    val colors: ColorPair,    // a, b
    val currentDeltaE: Float,
    val lastAnswer: LastAnswer?,
  ) : CMMatchUiState()
  data class Result(
    val score: Int,
    val correctCount: Int,
    val longestStreak: Int,
    val tightestDeltaE: Float, // minimum ΔE the player got right
    val results: List<RoundResult>,
    val gemsEarned: Int,
  ) : CMMatchUiState()
}

sealed class CMMatchUiEvent {
  data class Answer(val saidSame: Boolean) : CMMatchUiEvent()
  object Replay : CMMatchUiEvent()
  object NavigateHome : CMMatchUiEvent()
  object Share : CMMatchUiEvent()
}

enum class RoundResult { Correct, Wrong }
data class LastAnswer(val correct: Boolean, val truthSame: Boolean, val deltaE: Float)
data class ColorPair(val a: Color, val b: Color, val deltaE: Float, val isSame: Boolean)
```

---

## Color Math

**Use the existing `ColorEngine.kt` in `commonMain/domain/color/`. Do not re-invent.**

Add (or reuse) a method:

```kotlin
fun ColorEngine.generateMemoryPair(round: Int, isSame: Boolean): ColorPair {
  val base = randomVividColor()
  if (isSame) return ColorPair(base, base, 0f, true)
  val targetDeltaE = CM_MATCH_DELTA_E_CURVE[round - 1]  // see below
  val offset = offsetByDeltaE(base, targetDeltaE)
  return ColorPair(base, offset, targetDeltaE, false)
}

val CM_MATCH_DELTA_E_CURVE = listOf(5.0f, 4.0f, 3.0f, 2.5f, 2.0f, 1.5f, 1.2f, 1.0f, 0.7f, 0.5f)
```

`offsetByDeltaE` should perturb the base in CIE Lab space until `CIEDE2000(base, candidate)` is within ±5% of `targetDeltaE`. The prototype's JS version distributes the offset roughly 60% in L\* and 40% in a/b — feel free to do the same in Kotlin, but **measure the real ΔE with the existing CIEDE2000 implementation** and bisect/jitter until you hit the target. Don't approximate.

---

## Screens / Views

There is **one new screen** for the game (`CMMatchScreen`) plus reuse of `ResultScreen` with a new `gameType`.

### Screen: CMMatchScreen

Top-down layout inside a Box wrapped in `AmbientGlowBackground(primaryColor = HuezooColors.AccentPurple, secondaryColor = HuezooColors.AccentCyan)`:

1. **HuezooTopBar** — back button (parallelogram), wordmark `HUEZ`, info icon button on the right (opens `GameHelpSheet` for Memory Match).
2. **Sub-header strip** (12dp top padding, 20dp horizontal):
   - Left: `HuezooLabelSmall` "◉ GAME 6 · MEMORY MATCH" in `AccentPurple`
   - Right: `DeltaEBadge` showing the current round's ΔE, label "ROUND ΔE", aligned end
3. **Identity divider**: 3dp `AccentPurple` bar, 14dp top margin, 20dp horizontal margin
4. **GameHUD** (14dp top padding, 20dp horizontal):
   - Row of `SkewedStatChip`: `ROUND 01/10` (cyan accent), `SCORE 0` (yellow accent)
   - `RoundIndicator(total=10, current=round-1, results=results)` below
5. **Phase headline slot** (24dp horizontal, fixed min-height to prevent layout shift):
   - Phase tag: `HuezooLabelSmall` colored by phase (cyan for memory, yellow for hold, magenta for recall, green/magenta for feedback)
   - Title line: `HuezooTitleLarge`, white — phase-specific copy (see below)
6. **Central swatch area** (per variation — see "Variation A Layout" and "Variation B Layout" below)
7. **ΔE meta row** (24dp horizontal, baseline-aligned):
   - Left: small `HuezooLabelSmall` "THIS ROUND" + a `HuezooDisplaySmall` "ΔE x.x" colored by `DeltaEBadge.deltaEColor()` + tier label
   - Right (only during Feedback): `+10 PTS` (green) or `−5 PTS` (magenta)
8. **Action buttons** (20dp horizontal, 14dp top, 24dp bottom):
   - Row of two equal-flex `HuezooButton`s with 12dp gap:
     - `SAME` — `HuezooButton.Primary` when phase=Recall, `HuezooButton.Ghost` otherwise (disabled)
     - `DIFFERENT` — `HuezooButton.Danger` when phase=Recall, `HuezooButton.GhostDanger` otherwise (disabled)

### Phase headline copy

| Phase | Tag | Title (Vault) | Title (Twin Lock) |
|---|---|---|---|
| Memory | `◉ MEMORY PHASE` cyan | "Hold this color." | `◉ CHAMBER A LIVE` cyan / "Hold this color." |
| Hold | (only Vault: tag hidden, title only) | "Remember…" (pulsing) | `◉ CHAMBER A SEALED` yellow / "Stand by…" |
| Recall | `◉ RECALL` magenta | "Same color — or different?" | `◉ CHAMBER B LIVE — RECALL` magenta / "Match the seal?" |
| Feedback (correct) | `◉ LOCKED IN` green | sting copy (correct pool) | `◉ MEMORY VERIFIED` green / sting copy |
| Feedback (wrong) | `◉ EYE DRIFTED` magenta | sting copy (wrong pool) | `◉ MEMORY DRIFTED` magenta / sting copy |

### Variation A Layout — The Vault

Central swatch area: a 280×280 dp `Box`, centered.

- **Phase = Memory**:
  - `CountdownRing(size=280dp, segments=24, durationMs=3000, color=AccentCyan)` drawn behind the swatch.
  - `SwatchSquare(color=colors.a, size=220dp, accent=AccentCyan)` centered inside the ring.
  - Corner brackets in `AccentCyan.copy(alpha=.5)` at all 4 corners of the 280 box (tactical-frame feel).
- **Phase = Hold**:
  - `SealedChamber(size=220dp)` in place of the swatch. Diagonal-stripe pattern (cyan strokes @ 0.18 alpha), centered SEALED tag (blinking) + "HOLD" headline.
- **Phase = Recall**:
  - `SwatchSquare(color=colors.b, size=220dp, accent=AccentMagenta)` — magenta rim instead of cyan.
- **Phase = Feedback**:
  - `FeedbackCompare(colorA=colors.a, colorB=colors.b, truthSame, playerSaidSame, wrong)`:
    - Two 156×156 dp `ChamberMini` swatches side-by-side with 18dp gap
    - Center `=` (if truthSame) or `≠` glyph in `HuezooHeadlineLarge` size 36, colored green or magenta
    - Below: `HuezooLabelSmall` "TRUTH: SAME/DIFFERENT" (colored to match feedback)

### Variation B Layout — Twin Lock

Central area: a `Row` with two flex-1 chambers, 10dp gap, 320dp min-height, 20dp horizontal padding.

Each chamber is a `Box` with:
- Shelf offset 4×4 dp under it (`HuezooColors.SurfaceL4`)
- Face background = the chamber's current color (or `SurfaceL1` if locked/waiting)
- `rimLight()` modifier tinted with the accent color (`AccentCyan` for A, `AccentMagenta` for B)
- Top tag row (8dp inset): `HuezooLabelSmall` "CHAMBER A/B" (text uses `mixBlendMode = Difference` when over a color; in Compose, derive a contrast-safe text color from `Color.onColor()` instead) + LED indicator with state label (LIVE / SEAL / WAIT / OPEN)

Chamber states:

| State | Trigger | Visual |
|---|---|---|
| `Live` | Chamber A during Memory, Chamber B during Recall | Color visible, LED pulses in accent color |
| `Sealed` | Chamber A during Hold + Recall | Shutter overlay (vertical scanlines `SurfaceL2`/`SurfaceL1` stripes, yellow rim glow, blinking yellow "◉ SEALED" + "HOLD" headline) slides down from top with `scaleY(0→1, transform-origin top)` over 400 ms |
| `Waiting` | Chamber B during Memory + Hold | Diagonal stripe pattern in accent color @ 0.12 alpha, large "?" glyph in `HuezooDisplayLarge` size 96 in `accent.copy(alpha=.55)` |
| `Revealed` | Both chambers during Feedback | Color visible; Chamber A shows a bottom-centered "UNSEALED" pill (`HuezooLabelSmall`, white with `mixBlendMode = Difference`, dark transparent background) so players see it's the answer key |

The shutter open animation (Sealed → Revealed) is the reverse: `scaleY(1→0, transform-origin top)` over 350 ms.

---

## Result Screen

Reuse `ResultScreen.kt` / `ResultCard.kt` with a new `GameType.ColorMemoryMatch`.

Inside the existing card, render:

```
MISSION OUTCOME
{TIER_LABEL}                       ← title color depends on tier (see table above)

┌─ shelf 5×5 dp · rim AccentPurple ─┐
│  SCORE                       OF MAX │
│  {score} (60sp, cyan if isWin,    │
│   magenta if score<40)        100  │
│  ─────────────────────────────────  │
│  CORRECT      TIGHTEST ΔE          │
│  8/10  green  0.50  yellow         │
│  STREAK       GEMS                 │
│  3     cyan   +16  yellow          │
│  ─────────────────────────────────  │
│  ROUND-BY-ROUND                    │
│  [10 segments, green/magenta]      │
│  ─────────────────────────────────  │
│  Sharp. Very sharp.  (sting copy)  │
└────────────────────────────────────┘

[ HOME (ghost) ]  [ PLAY AGAIN (primary) ]
              ↗ SHARE SCORE CARD (yellow)
```

Confetti fires only if `score >= 40` (matches existing rule in `GAME_DESIGN.md` §Confetti Rules — non-zero score). Identity color for confetti = `AccentPurple`.

---

## Sting Copy Pool

This is canonical — copy verbatim into a new `ui/copy/CMMatchStingCopy.kt`.

### On correct answer

| Pool | Trigger (ΔE of round) | Lines |
|---|---|---|
| Easy | ≥ 3.5 | `"Locked in."` `"Eyes still warm."` `"Steady."` |
| Mid | 2.0 – 3.5 | `"Sharp."` `"Eye remembered."` `"Held the line."` |
| Hard | 1.0 – 2.0 | `"Real recall."` `"That one was real."` `"Eyes did not blink."` |
| Elite | < 1.0 | `"Beyond impressive."` `"Your retina is a recorder."` `"Eyes elite."` |

### On wrong answer (always inject the actual ΔE)

| Pool | Trigger | Lines |
|---|---|---|
| Easy | ≥ 3.5 | `"That gap was wide. Hmm."` `"Warmer than that."` `"The eye wandered."` |
| Mid | 2.0 – 3.5 | `"Close, but ΔE {de} got you."` `"Memory blinked at ΔE {de}."` `"Almost. ΔE {de}."` |
| Hard | 1.0 – 2.0 | `"ΔE {de}. Sub-pixel territory."` `"You were right there. ΔE {de}."` `"Memory faded at ΔE {de}."` |
| Elite | < 1.0 | `"ΔE {de}. Barely real. Still missed."` `"Below human limits. So is the gap."` `"You almost out-saw your own eye."` |

`{de}` formats to 1 decimal for ΔE ≥ 1.0, 2 decimals for ΔE < 1.0.

### Result screen sting (by total score)

```kotlin
fun resultStingByScore(score: Int): String = when {
  score >= 95 -> "Superhuman recall. Seriously."
  score >= 80 -> "Your memory is elite."
  score >= 60 -> "Sharp. Very sharp."
  score >= 40 -> "Better than most."
  score >= 20 -> "Room to grow."
  score > 0   -> "Keep training."
  else        -> "The eye drifted today. Come back."
}
```

### Help-sheet copy (for `GameHelpSheet`)

| Section | Copy |
|---|---|
| THE GOAL | A color appears for 3 seconds. The chamber seals. A second color appears. Are they the same — or different? |
| 10 ROUNDS | Each round the ΔE shrinks. Round 1 is obvious. Round 10 is sub-perceptual. |
| SCORING | +10 per correct round. −5 per wrong. Max score 100. |
| MEMORY | The first color disappears after 3 seconds. No second look. |
| GEMS | +2 per correct round. Streak bonuses. +25 perfect-run bonus. |

---

## Interactions & Behavior

### Animations

| What | Where | Duration | Easing |
|---|---|---|---|
| Swatch appears | Memory + Recall phase swatch entrance | 420 ms | `scale 0.85 → 1.05 → 1.0` (spring overshoot, `cubic-bezier(.34,1.56,.64,1)`) |
| Countdown ring drain (Variation A) | Memory phase | 3000 ms | Linear; per-segment alpha flip from `color` to `color.copy(alpha=.08)` as time passes the segment threshold |
| Sealed chamber stripe pulse | Hold phase (Vault & Twin Lock) | 1200 ms loop | `opacity 1 ↔ .25`, ease-in-out (matches existing `hz-blink` pattern) |
| Shutter close (Variation B) | Memory → Hold | 400 ms | `scaleY 0 → 1, transform-origin: top`, `cubic-bezier(.6,0,.4,1)` |
| Shutter open (Variation B) | Recall → Feedback (Chamber A only) | 350 ms | `scaleY 1 → 0, transform-origin: top`, `cubic-bezier(.6,0,.4,1)` |
| Wrong-answer shake | After wrong tap, on the chosen-but-wrong button | 450 ms | `translateX` shake — reuse `SwatchBlock`'s existing `ShakeX` animation |
| Result card entrance | Phase → Result | 550 ms | Slide up 60 dp + scale 0.92 → 1, `cubic-bezier(.16,1,.3,1)` (reuse existing `ResultCard` entrance) |
| Confetti | Result if `score >= 40` | 1200 ms | 22 particles, radial spread 80–200 dp, rotation ±360° (reuse existing confetti) |

### Input rules

- During Memory / Hold / Feedback phases, `SAME` and `DIFFERENT` buttons render as `Ghost` / `GhostDanger` (border only) and are **disabled** (no tap).
- During Recall, both buttons enable. Tap immediately transitions to Feedback.
- Back button during a session: prompt `HuezooBottomSheet` "Leave session?" — if confirmed, navigate back; session is **not** saved (a session is only counted toward leaderboard/stats if all 10 rounds complete).
- No timeout on Recall in MVP. (If we later add one, it lives in `CMMatchViewModel` and ticks down via a `CountdownRing` on the SAME/DIFFERENT button row.)

---

## Design Tokens

All values use existing tokens. Do **not** introduce new color tokens.

### Colors

| Token | Hex | Usage |
|---|---|---|
| `HuezooColors.AccentPurple` | `#9B5DE5` | Game 6 identity — sub-header tag, identity divider, AmbientGlowBackground primary, ResultCard rim |
| `HuezooColors.AccentCyan` | `#00E5FF` | Memory-phase accent, Chamber A rim, SAME button face, Result CORRECT bar accent |
| `HuezooColors.AccentMagenta` | `#FF2D78` | Recall accent, Chamber B rim, DIFFERENT button face, wrong-answer sting tag |
| `HuezooColors.AccentYellow` | `#FFE600` | SEALED tag (Hold + shutter), SCORE chip, SHARE button face |
| `HuezooColors.AccentGreen` | `#00F5A0` | LOCKED IN tag, +10 PTS, correct-result dots |
| `HuezooColors.Background` / `SurfaceL0` – `SurfaceL4` | per `Color.kt` | All surface treatments |
| `HuezooColors.TextPrimary` / `TextSecondary` / `TextDisabled` | per `Color.kt` | All text — never `MaterialTheme.colorScheme.*` |

### Typography (existing scale, no new sizes)

| Slot | Composable |
|---|---|
| Phase tag | `HuezooLabelSmall` |
| Phase title (Vault / Twin Lock) | `HuezooTitleLarge` |
| ΔE inline number | `HuezooDisplaySmall` |
| ΔE tier label | `HuezooLabelSmall` |
| HUD chip number | `HuezooDisplaySmall` (24sp via existing chip override) |
| HUD chip label | `HuezooLabelSmall` |
| Result tier header (`STRONG RECALL` etc.) | `HuezooHeadlineMedium` |
| Result score (`70`) | `HuezooDisplayLarge` at 72sp (existing override pattern in `ResultCard`) |
| Result sting copy | `HuezooHeadlineMedium` |
| Buttons | `HuezooLabelLarge` (built into `HuezooButton`) |

### Spacing

Use `HuezooSpacing.{xs, sm, md, lg, xl}` everywhere. The prototype's 14 / 20 / 24 px values map to `sm`/`md`/`lg` — confirm exact mapping in `Dimensions.kt` and adjust.

### Component reuses (no new components needed besides the ones flagged)

- `HuezooTopBar` — game-screen variant (parallelogram back button + info icon)
- `SkewedStatChip` — ROUND, SCORE (existing component, just two instances)
- `RoundIndicator` — 10 dots, results map directly
- `HuezooButton` — Primary, Danger, Ghost, GhostDanger variants
- `DeltaEBadge` — existing component, used for round ΔE display
- `AmbientGlowBackground` — wraps the whole screen
- `ResultCard` / `ResultScreen` — extend with `GameType.ColorMemoryMatch`
- `HuezooBottomSheet` — back-confirm prompt

### New components to create

| Composable | File | Purpose |
|---|---|---|
| `CMMatchScreen` | `ui/games/colormemory/CMMatchScreen.kt` | Top-level screen |
| `CMMatchViewModel` | `ui/games/colormemory/CMMatchViewModel.kt` | State machine + timers + scoring |
| `CMMatchUiState` / `CMMatchUiEvent` / `CMMatchNavEvent` | `ui/games/colormemory/state/` | State + event sealed classes |
| `CountdownRing` | `ui/components/CountdownRing.kt` | Segmented arc that drains over a duration. Variation A only. Composable signature `(modifier, size, segments, durationMs, color, active)`. |
| `MemoryChamber` | `ui/components/MemoryChamber.kt` | Twin Lock chamber. States: `Live`, `Sealed`, `Waiting`, `Revealed`. Variation B only. |
| `ChamberShutter` | `ui/components/ChamberShutter.kt` | Animated vertical-stripe shutter overlay. Variation B only. |
| `SealedChamberPanel` | `ui/components/SealedChamberPanel.kt` | The Vault's Hold-phase replacement panel. Variation A only. |
| `FeedbackCompare` | `ui/components/FeedbackCompare.kt` | Side-by-side A/B mini-chambers with `=` / `≠` glyph. Variation A only. |
| `CMMatchStingCopy` | `ui/copy/CMMatchStingCopy.kt` | Sting copy pools + pickers (deterministic seed by round, OR random) |

Only build the components for the variation you pick. If you pick A, skip `MemoryChamber` + `ChamberShutter`. If you pick B, skip `CountdownRing` + `SealedChamberPanel` + `FeedbackCompare` (the in-place chamber reveal replaces the side-by-side comparison).

### Navigation

Add to `navigation/`:

```kotlin
@Serializable object ColorMemoryMatchGame
```

Wire in `App.kt`:

```kotlin
composable<ColorMemoryMatchGame> {
  CMMatchScreen(
    onNavigate = { navEvent -> /* same pattern as Threshold/Daily */ }
  )
}
```

Add the home-screen card. The post-MVP Home layout already mentions a Color Memory tile (see `docs/VISION.md` "Full Home Screen (post-MVP)"). Match the existing `Daily` / `Leaderboard` card style — full-width Row, identity color accent (`AccentPurple`), best-score subtitle.

---

## State persistence

- **Personal best**: best total score across all 10-round sessions. Store via `SettingsRepository` (or a new `ColorMemoryRepository` modeled on `ThresholdRepository`). Update on every completed session if `score > current.best`.
- **Tightest ΔE**: minimum ΔE the player got *correct* (not the round's target ΔE if they missed it). Used in ResultCard "TIGHTEST ΔE" stat.
- **Leaderboard**: out of scope for MVP. Per `VISION.md` Firebase data model, this game ships its own `/leaderboard/color_memory` board in Phase 8.

Do **not** persist mid-session state (a session is atomic — leaving aborts). This matches Daily's behavior.

---

## Assets

No new image assets. All visuals are Canvas-drawn (countdown ring, shutter stripes, corner brackets) or composed of existing tokens.

The Threshold game's `ThresholdScannerIllustration` is a useful reference for the canvas-drawing style — see `docs/DESIGN_SYSTEM.md` §9 "Illustration System".

---

## Files in this bundle

```
design_handoff_color_memory_match/
├── README.md                         ← you are here
└── prototype/
    ├── Color Memory Match.html       ← open this in a browser
    ├── color-memory-match.jsx        ← main app + variation flows
    ├── huezoo-ui.jsx                 ← web port of Huezoo tokens + components
    └── tweaks-panel.jsx              ← live design-tweak panel (engineering doesn't need this)
```

---

## Implementation Sequence

Recommended order:

1. **Pick a variation** with the designer in code review (this is the single biggest decision and changes which files you create).
2. Add `CM_MATCH_DELTA_E_CURVE` and `generateMemoryPair` to `ColorEngine.kt`. Write unit tests using the existing CIEDE2000 — verify the produced pair has the requested ΔE within tolerance.
3. Scaffold `CMMatchViewModel` with the phase state machine. Drive it with fake colors first; unit-test phase transitions and scoring (correct/wrong/streak bonuses).
4. Build the chosen variation's component (`CountdownRing` for A, `MemoryChamber` + `ChamberShutter` for B). Style-pass against the HTML prototype.
5. Wire `CMMatchScreen` to the ViewModel. Snapshot-test each phase.
6. Extend `ResultCard` for `GameType.ColorMemoryMatch` — confirm the sting copy + score gating works.
7. Add the home-screen card.
8. Add the `GameHelpSheet` copy.
9. Wire `PlatformOps.shareText` for the share button (same call site as the other games).
10. Add Firebase Analytics events: `cm_match_session_start`, `cm_match_round_completed`, `cm_match_session_completed` with `score`, `correctCount`, `longestStreak`.

---

## Open Questions for Designer Review

1. **Variation A vs B** — final pick.
2. **Memory time** — 3.0 s feels right at default ΔE but late-game (round 9–10, ΔE 0.5) the player may want longer. Consider a tuning per-round or accessibility setting. MVP: 3.0 s flat.
3. **Recall timeout** — none in MVP. If we add one, suggest 5 s with a thin countdown bar above the SAME/DIFFERENT row. Timing out counts as wrong.
4. **Streak bonus thresholds** — proposed +5 at 3 streak and +10 at 5 streak. Confirm before code.
5. **Daily seeding** — the Daily Challenge is date-seeded; should Color Memory Match also have a daily seeded mode? Or is this purely a free-play game? VISION.md treats it as free-play; assumed unless told otherwise.
