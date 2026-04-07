# The Perception Wall — Design Doc

*Feature: what happens when a player correctly identifies ΔE = 0.1 (the floor)*

Status: **Implemented — Phases 1–3 complete** (April 2026)

---

## The Problem

`ThresholdGameEngine.MIN_DELTA_E = 0.1` is the current floor.
When a player answers correctly at 0.1, the game currently silently stays at 0.1
forever (`coerceAtLeast`) until they eventually miss. That is a missed dramatic
opportunity — reaching 0.1 is a genuinely exceptional achievement.

Human color discrimination limits under ideal conditions: ~0.5–1.0 ΔE.
Below 0.5 is statistically indistinguishable for most humans.
At 0.1 it is essentially a coin flip. A machine, or an extraordinarily lucky
human, can get there.

---

## The Mechanic

When a player correctly identifies the odd swatch at `ΔE ≤ MIN_DELTA_E`:

1. **That life ends in triumph** — not failure.
   The perception wall has been broken. A special "LEGENDARY" celebration fires
   (distinct from a normal correct tap).

2. **New life starts from `STARTING_DELTA_E` (5.0)** — same as after a wrong
   answer, but with full pomp instead of failure animation.
   Tries remaining is decremented (same as a wrong answer), because the life
   is truly over — you can't go lower.

3. **`hitPerceptionWall = true`** is stored in session state — used later on
   the result screen.

This means players can still use remaining tries after hitting the wall.
The wall is not game-over — it is a milestone within a session.

---

## UI Moments

### 1. In-game "Wall Hit" celebration (new state: `RoundPhase.PerceptionWall`)

Sequence (replaces the normal correct-tap flow when at the floor):

```
Correct tap at MIN_DELTA_E
  → All swatches flash gold/white (not the normal green)
  → Eagle mascot animates in (large, centered, ~1.5s)
  → Copy overlay:
      Line 1: "PERCEPTION LIMIT REACHED"
      Line 2: "ΔE 0.1 — Beyond human vision"
  → Haptic: long sustained rumble (distinct from normal correct)
  → After ~2.5s: fold out, new life starts at 5.0
```

### 2. Result screen — Neon Border (conditional)

The neon rectangular border (inspired by `docs/ideas/screen.png`) appears
**only** when `hitPerceptionWall = true` in the session result.

- Normal result: current card design, no border
- Legendary result: neon glowing border (cyan → magenta → yellow gradient,
  animated shimmer on entry). Same screen layout, same stats — just the border
  transforms the visual weight of the moment.
- Special copy beneath ΔE: `"PERCEPTION LIMIT REACHED"` badge (small, gold)

This makes the neon border **earned** and **rare**. Most players will never see
it. Those who do will screenshot it.

---

## Animal Mascots — Difficulty Tiers

Each ΔE range surfaces a different animal watermark inside the swatch.
Appears as a faint icon (low opacity, centered, non-interactive).
Also flashes briefly on correct-answer reveal (full opacity, 300ms).

| ΔE range | Animal | Character |
|---|---|---|
| 5.0 → 3.0 | 🐻 Bear | Relaxed, warm |
| 3.0 → 2.0 | 🦊 Fox | Alert, clever |
| 2.0 → 1.0 | 🐸 Frog | Precise, focused |
| 1.0 → 0.5 | 🐙 Octopus | Strange, exceptional |
| < 0.5 | 🦅 Eagle | Legendary, beyond human |

The Eagle is the only animal that appears at the perception wall.
It should feel like encountering something rare.

Assets needed: 5 animal icons (line-art style, single color, works on any
colored swatch background). SVG or vector preferred for crispness at small sizes.

---

## Session Result Changes

`SessionResult` (or the ViewModel's result cache) gains:
```kotlin
val hitPerceptionWall: Boolean = false
```

Set to `true` in `handleCorrectTap()` when `currentDeltaE <= MIN_DELTA_E`.

`ResultUiState.Ready` gains:
```kotlin
val isLegendaryResult: Boolean = false
```

Result screen reads this flag to conditionally show the neon border.

---

## Build Order

1. **Phase 1 — Wall mechanic** (ViewModel + game flow only, no new UI)
   - Detect floor hit in `handleCorrectTap`
   - Award **5000 gems** instantly on wall hit
   - End life triumphantly (decrement tries, reset ΔE, store flag)
   - Add `RoundPhase.PerceptionWall`
   - Add `hitPerceptionWall` to session result

2. **Phase 2 — In-game celebration UI**
   - Eagle overlay animation + copy
   - Gold swatch flash
   - Distinct haptic

3. **Phase 3 — Result screen neon border**
   - Conditional neon border composable (animated shimmer)
   - `"PERCEPTION LIMIT REACHED"` badge on ΔE stat card
   - Only renders when `isLegendaryResult = true`

4. **Phase 4 — Animal mascots**
   - Needs asset delivery first (5 SVG icons)
   - Watermark in swatch composable, keyed on current ΔE range
   - Flash on correct reveal

---

## Multi-wall sessions (clarification added April 2026)

A player can reach MIN_DELTA_E in more than one try within a single session:

- **Life always ends at MIN_DELTA_E** — regardless of whether `hitPerceptionWall`
  is already `true`. The player is never stuck in an infinite loop at ΔE 0.1.
- **5 000-gem bonus is awarded at most once per session** — even if the player
  reaches MIN_DELTA_E in every try. The first wall hit sets `hitPerceptionWall = true`;
  subsequent wall hits skip the gem award but still end the life triumphantly.
- **`hitPerceptionWall` in `SessionResult`** is `true` for the entire session if the
  wall was hit at least once; no per-try tracking is stored.

This is covered by `ThresholdPerceptionWallTest` in the test suite.

---

## Decisions (locked April 2026)

- **Wall consumes a try** — life is over, can't go lower. Rewarded with 5000 gems.
- **Neon border is always cyan/magenta/yellow** — fixed, iconic, matches promo art.
  Makes the legendary state instantly recognizable regardless of round color.
  Colors flow *around* the border (phase-shifted sweep gradient); the rectangle
  itself never rotates.
- **Eagle appears at ΔE < 0.5** — not only at the exact floor. The 0.5 crossing
  is where human perception ends; Eagle marks that whole territory.
- **`hitPerceptionWall` leaderboard badge** — persist to DB, future work.
