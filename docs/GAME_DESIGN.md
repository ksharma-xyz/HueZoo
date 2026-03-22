# Huezoo — Game Design Reference

*Single source of truth for game rules, scoring, gem economy, and player levels.*
*All values are implemented in code — change here, then update the code to match.*

---

## The Two Games

### The Threshold

**Concept**: Detect the smallest color difference you can. One miss per try. Best ΔE across all tries wins.

**Structure**:
```
Session = up to N tries (N = 10 free / unlimited paid)
Try     = one run from ΔE 5.0 until the first wrong tap
Tap     = one correct answer within a try
```

**ΔE progression within a try**:
- Starts at ΔE 5.0
- Drops by 0.3 on each correct tap
- Floor: ΔE 0.1 (absolute limit of the binary search)
- Base color changes every tap (visual variety, same difficulty)

**On wrong tap**:
- Reveal the correct swatch
- Show sting copy
- Burn 1 try — ΔE resets to 5.0, new base color, continue
- Milestones reset (each try earns milestones independently)
- **Personal best update**: if the player has at least one correct tap this session (`bestDeltaE != null`), the *attempted* `currentDeltaE` is saved as a personal best candidate — reaching a level counts even if you fail it. Guards against recording the trivial 5.0 start value when someone taps wrong on their very first round.

**Session end** (all tries exhausted):
- Navigate to Result with `bestDeltaE` = lowest ΔE reached (correctly identified or attempted after at least one correct tap) across all tries
- Score = `1000 / bestDeltaE` (floored at ΔE 0.3 → max score ≈ 3333)

**Personal best persistence**:
- Saved **eagerly on every correct tap** (inside `withContext(NonCancellable)`) — survives back-press/drop-out mid-session
- Saved on wrong tap (see above) — the reached level counts, not just the last correctly passed level
- DB write uses the repo's own `isNewBest` guard (`deltaE < current.best_delta_e`) so only genuine improvements are persisted

**HUD labels**:
- `TAP X` — correct-tap count in the current try
- `X TRIES LEFT` — remaining tries in the 8-hour window

**Attempt window**:
- **Free**: 10 tries per 8-hour window. When exhausted, player waits for the cooldown before a fresh batch.
- **Paid**: 10 tries per batch, no cooldown. When a batch is exhausted the session is cleared immediately and a fresh batch begins — paid users always return to `Available`. The result screen still fires after each batch (try-tracking is local to the ViewModel, not re-fetched from the DB).

---

### Daily Challenge

**Concept**: Date-seeded puzzle, same for every player worldwide. 1 attempt per day. All 6 rounds are always played — no early exit on wrong tap.

**Structure**:
```
6 rounds, fixed ΔE curve: [4.0, 3.0, 2.0, 1.5, 1.0, 0.7]
- Correct tap → earn points for that round, advance
- Wrong tap   → 0 points for that round, reveal correct swatch, advance
- After round 6 → always navigate to Result
```

**Scoring**:
- Per correct round: `scoreFromDeltaE(deltaE)` = `1000 / deltaE`
- Wrong rounds score 0
- Total = cumulative across all 6 rounds

**Result fields**:
- `roundsSurvived` = count of rounds answered correctly (0–6)
- `deltaE` = ΔE of the last round played (round 6, always ΔE 0.7)
- `score` = cumulative score from correct rounds

**PersonalBest**: best cumulative score ever recorded.

---

## Gem Economy

All rates live in `GameRewardRates.kt` — edit there, not in ViewModels.

### Threshold earn rates

| Event | Gems |
|---|---|
| Correct tap | +2 |
| First time ΔE < 2.0 in a try *(SHARP milestone)* | +5 |
| First time ΔE < 1.0 in a try *(EXPERT milestone)* | +10 |
| First time ΔE < 0.5 in a try *(ELITE milestone)* | +25 |

Milestone bonuses are per-try — cleared when a new try starts (wrong tap). A player who reaches ΔE 1.0 in three different tries earns the EXPERT bonus three times total.

### Daily earn rates

| Event | Gems |
|---|---|
| Each correct round | +5 |
| Completing all 6 rounds *(participation)* | +3 |
| All 6 rounds correct *(perfect)* | +20 bonus |

**Maximum per Daily session** (perfect run): 5×6 + 3 + 20 = **53 gems**
**Minimum per Daily session** (0/6 correct): 3 gems (participation only)

---

## Player Levels

Based on lifetime gems accumulated. All thresholds in `PlayerLevel.kt`.

| Level | Min gems | Color | Approx sessions* |
|---|---|---|---|
| ROOKIE | 0 | Cyan | — |
| TRAINED | 150 | Green | ~10 |
| SHARP | 750 | Magenta | ~50 |
| ELITE | 5,000 | Yellow | ~333 |
| MASTER | 50,000 | Gold (#FFB800) | ~3,333 |

*\* At ~15 gems/session average (mix of Threshold + Daily)*

---

## ΔE Tier Reference

Used for sting copy, badge labels, and milestone bonuses.

| ΔE range | Perception | Label | Milestone bonus |
|---|---|---|---|
| 5.0 – 3.0 | Easily visible | BEGINNER | — |
| 3.0 – 2.0 | Noticeable with focus | TRAINING | — |
| 2.0 – 1.0 | Hard | SHARP | +5 gems at < 2.0 |
| 1.0 – 0.5 | Expert / colorist level | EXPERT | +10 gems at < 1.0 |
| 0.5 – 0.1 | Near human limits | ELITE | +25 gems at < 0.5 |

---

## Scoring Formula

```kotlin
fun scoreFromDeltaE(deltaE: Float): Int = (1000f / deltaE.coerceAtLeast(0.3f)).toInt()
```

| ΔE | Score |
|---|---|
| 5.0 | 200 |
| 3.0 | 333 |
| 2.0 | 500 |
| 1.0 | 1,000 |
| 0.5 | 2,000 |
| 0.3 | 3,333 (cap) |

---

## Confetti Rules

Confetti fires only when there's something worth celebrating:
- `score > 0` — no confetti for a zero-point result
- Zero-score result: banner reads "MISSION OUTCOME: FLATLINED", hero score in `AccentMagenta`

*(Future: refine thresholds once real score distribution is known from prod data.)*

---

## Testability Notes

All reward logic is isolated for unit testing:

- **`GameRewardRates`** — pure constants object, no dependencies
- **`ThresholdViewModel`** — `checkAndAwardMilestone()` is private but its effects are visible via `sessionGems` passed to `Result`
- **`DailyViewModel`** — `correctRounds`, `sessionGems`, `cumulativeScore` all flow into `Result` nav args
- **`PlayerLevel.fromGems()`** — pure function, trivially testable

To test ViewModels: inject fake `ColorEngine`, `ThresholdRepository`, `DailyRepository`, `SettingsRepository` implementations and drive events via `onUiEvent()`.

---

## Player-Facing Game Explanation

*This is the copy used in the in-game `?` help sheets (`GameHelpSheet.kt`). Keep this in sync when rules change.*

### The Threshold — player copy

| Section | Copy |
|---|---|
| THE GOAL | Six swatches appear — one has a slightly different hue. Tap the outlier. |
| TAPS | Each correct tap makes the colour difference smaller by ΔE 0.3. The longer your streak, the harder it gets. |
| TRIES | One wrong tap ends the current try — ΔE resets to 5.0 and a new run begins. You get 10 tries per session. Your best ΔE reached across all tries is your result — reaching a level counts even if you miss it. |
| WHAT IS ΔE? | ΔE measures colour difference. ΔE 5.0 = easy / ΔE 2.0 = trained / ΔE 1.0 = expert / ΔE 0.5 = near human limits. |
| GEMS | +2 per tap. Milestone bonuses: ΔE < 2.0 → +5 / ΔE < 1.0 → +10 / ΔE < 0.5 → +25. Reset each try. |

### Daily Challenge — player copy

| Section | Copy |
|---|---|
| THE GOAL | Six swatches appear — one has a slightly different hue. Tap the outlier. |
| 6 ROUNDS, ALWAYS | Fixed ΔE curve: 4.0 → 3.0 → 2.0 → 1.5 → 1.0 → 0.7. Wrong tap reveals correct swatch and advances — all 6 always play. |
| SCORING | Correct rounds score 1000 ÷ ΔE. Wrong rounds score 0. Total is cumulative. |
| SAME PUZZLE WORLDWIDE | Date-seeded — same colours for every player. One attempt per day. |
| GEMS | +5 per correct round. +3 participation (always). +20 perfect-run bonus (all 6 correct). |

---

## Planned Screens (TODO)

### First-Launch Onboarding

Show a brief walkthrough on the very first app open, before the Home screen:
- Slide 1: "What is ΔE?" — one sentence + visual (two swatches, delta label)
- Slide 2: Threshold rules in 3 bullet points
- Slide 3: Daily rules in 3 bullet points
- Skip button always visible

**Implementation notes:**
- Gate on a `SettingsRepository.hasSeenOnboarding(): Boolean` flag (store in SQLDelight settings table or DataStore).
- Render as a full-screen overlay on top of `HomeScreen` (not a separate nav destination) so nav stack stays clean.
- After dismiss, set flag so it never shows again.

---

## Open Design Questions

1. **Threshold try count**: currently 5 free / 10 free (see `MAX_ATTEMPTS_RELEASE`). Confirm final value before monetization implementation.
2. **Score for wrong Daily round**: currently 0. Alternative: partial credit (e.g. 20% of round score for "at least attempting"). TBD.
3. **Confetti cutoff for Daily**: currently any gemsEarned > 0. Future: only if ≥ 4/6 correct?
4. **Swatch size difficulty**: larger swatches = easier to see difference. Planned as an accessibility/difficulty setting. Implementation note: ΔE is the core difficulty lever; swatch size is a secondary modifier (1.0x default, 1.2x easier, 0.85x harder). Wire through `UserSettings` when implemented.

---

## Planned Screens (TODO)

### Out of Tries — Refill Sheet

Design reference: `docs/stitch_huezoo_prd_design_doc/huezoo_refill_out_of_tries/code.html`

A bottom sheet / modal shown when the player's try budget is exhausted mid-session or on entry to Threshold when blocked.

**Key elements:**
- Header: "OUT OF TRIES" / "Energy Depleted" — AccentMagenta
- Gem balance display (current inventory)
- Two refill options (stacked):
  1. **Gem refill** — e.g. 300 gems for 10 tries. Disabled + insufficient-gems warning when player can't afford it.
  2. **Watch Ad** — free refill via rewarded video (always available). AccentCyan CTA.
- Close button (top-right)
- 3D shelf button styling matching existing `HuezooButton`

**Implementation notes:**
- Replace current `ThresholdUiState.Blocked` full-screen with a nav-modal version, or layer the sheet over the blocked screen.
- Ad integration is future work — wire up the Watch Ad button with a stub callback for now.
- Gem deduction should go through `SettingsRepository.addGems(-300)` (or a dedicated method).

---

### Levels & Progress Sheet

Design reference: `docs/stitch_huezoo_prd_design_doc/huezoo_levels_progress/code.html`

A bottom sheet accessible from the Home screen (tap the level badge / gem pill area) showing the full 5-tier progression system.

**Key elements:**
- Header: "LEVELS & PROGRESS" + current level badge ("LVL 1 — ROOKIE")
- Scrollable level cards — one per tier, color-coded by `PlayerLevel.levelColor`:
  - Rookie (Cyan) | Trained (Green) | Sharp (Magenta) | Elite (Yellow) | Master (Gold)
  - Each card shows: level name, gem threshold, "what changes" section
- Current level highlighted / expanded; locked tiers shown at reduced opacity
- Progress bar: gems toward next tier
- Close button (full-width primary) at bottom

**"What changes" per level (to define):**
- Threshold difficulty curve, reward multipliers, cosmetic unlocks (badge frame, aura) — TBD with monetization plan.

**Implementation notes:**
- `PlayerLevel.fromGems(n)` already exists — use it for current level + next threshold.
- Trigger: tap on `CurrencyPill` in `HuezooTopBar` (or a dedicated level badge on Home).
- Sheet state: `ModalBottomSheet` (M3) or a custom slide-up composable matching the app's design language.
