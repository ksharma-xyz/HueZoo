# Huezoo — LLM Implementation Plan (Levels, Economy, UX)

This document is a planning spec for another LLM to implement features in this codebase.
It is intentionally implementation-ready, but it is not code.

Use this with:
- `docs/MVP.md`
- `docs/ux.md`
- `docs/TESTING.md`
- `docs/DESIGN_SYSTEM.md`

---

## 1) Objective

Align Huezoo's brand expression, level progression, gem economy, and gameplay motion into one cohesive loop:
- clearer difficulty progression
- stronger visual identity (`Hue` solid + `Zoo` outline)
- stable UI layout (no shifting feedback areas)
- monetization that feels fair and understandable

---

## 2) Scope

### In scope
- Top-left brand lockup behavior in app chrome
- Splash animation sequence at app start
- Level system (3 levels) and level-explanation bottom sheet
- Top bar level color behavior as player progresses
- Gem earning/spending logic redesign
- Ad display policy tied to attempts and gem depletion
- Alternative swatch layouts (flower-petal and other shapes)
- Fold/unfold transition choreography between rounds
- Fixed-height feedback area for correctness messages

### Out of scope (for this plan)
- Full visual redesign of all screens
- Backend schema overhaul beyond required counters
- New game modes beyond Threshold and Daily

---

## 3) Codebase Conventions (must follow)

- Platform: Kotlin Multiplatform + Compose Multiplatform
- Shared app logic/UI lives in `composeApp/src/commonMain/`
- Prefer ViewModel-driven state; avoid UI-local business logic
- Keep side effects behind interfaces; add fakes in `commonTest`
- Reuse existing design system tokens/components before adding new primitives
- Keep constants configurable; avoid hard-coded magic numbers

Primary likely touchpoints:
- `composeApp/src/commonMain/kotlin/xyz/ksharma/huezoo/ui/components/HuezooTopBar.kt`
- `composeApp/src/commonMain/kotlin/xyz/ksharma/huezoo/ui/components/SwatchBlock.kt`
- `composeApp/src/commonMain/kotlin/xyz/ksharma/huezoo/ui/games/threshold/ThresholdViewModel.kt`
- `composeApp/src/commonMain/kotlin/xyz/ksharma/huezoo/ui/games/threshold/state/ThresholdUiState.kt`
- `composeApp/src/commonMain/kotlin/xyz/ksharma/huezoo/ui/games/daily/DailyViewModel.kt`
- `composeApp/src/commonMain/kotlin/xyz/ksharma/huezoo/ui/theme/Shape.kt`

---

## 4) Product Behavior Spec

## 4.1 Brand lockup and top bar text

### Rule
- Display brand as `HueZoo` with:
  - `Hue`: solid fill
  - `Zoo`: outline stroke

### Level-linked top bar behavior
- Default/top-level state: outlined style emphasis
- As player level increases, level accent color appears in lockup treatment
- Tap on top bar lockup opens a bottom sheet explaining levels and thresholds

### Bottom sheet content (minimum)
- Level name
- Color identity
- Entry threshold
- What changes at this level (difficulty, rewards, prestige)

---

## 4.2 Splash animation behavior

At app launch:
1. Show `HueZoo` wordmark where `Hue` is solid and `Zoo` is outline
2. Simulate light flicker inside `Zoo` letters (outline interior illumination)
3. Cycle through 3 level colors one by one
4. End in the player's current level color (or Level 1 color for new users)

Animation intent:
- Feels neon/electric, not chaotic
- Short and skippable by timeout; avoid long blocking intro

---

## 4.3 Three-level system (proposed)

Your target says Level 3 should feel very hard and around 50,000 correct responses.
Use a two-part model: immediate starter progression + long-tail mastery.

### Recommended thresholds (proposal A)
- Level 1 (Rookie): `0-199` correct
- Level 2 (Skilled): `200-49,999` correct
- Level 3 (Master): `50,000+` correct

Why this works:
- Matches your requirement (`~200` first jump, `50,000` hardest)
- Creates early achievement then very long aspirational grind

### Alternative thresholds for better pacing (proposal B)
- Level 1: `0-499`
- Level 2: `500-9,999`
- Level 3: `10,000+`

Use proposal B if retention data shows proposal A is too extreme.

### Color identities (define 3 colors)
- Level 1: Electric Cyan
- Level 2: Neon Magenta
- Level 3: Solar Gold

These colors should be reused in:
- splash sequence
- top bar lockup accent
- level chips/badges
- level info bottom sheet

---

## 4.4 Gem economy redesign

User request constraints:
- earn `1 gem` per correct answer
- when out of tries, `300 gems` should buy `10 questions`

### Proposed economy model

#### Earning gems
- +1 gem per correct answer
- Daily first-win bonus: +20 gems (once per day)
- Streak milestone bonus:
  - 5 streak: +5 gems
  - 10 streak: +15 gems

#### Spending gems
- `300 gems -> 10 question pack` (Threshold-only recovery pack)
- Pack purchase shown only when player is out of tries or voluntarily taps "Refill"

#### Ad policy
- Rewarded ad available on depletion:
  - Option 1: +1 try OR +30 gems
- Interstitials capped and non-disruptive:
  - at most one interstitial every N sessions (start with N=3)
- Never interrupt active gameplay with forced ad mid-round

#### Fairness controls
- Keep paid users ad-free (if existing purchase state supports it)
- Show transparent economy math in UI:
  - gems owned
  - gems needed for next refill
  - exact refill conversion

---

## 4.5 Gameplay presentation changes

## A) Non-row layouts
Current issue: fixed square swatches in one row feel static.

Introduce layout variants (rotate by round/theme):
1. Flower layout: 6 petal-like swatches in radial arrangement (odd one out mechanic unchanged)
2. Orbit layout: 5-7 circular nodes around center
3. Diamond cluster: 4 corners + center

## B) Fold/unfold motion
Round transition choreography:
1. Current shape unfolds (petals/nodes appear one-by-one in circular order)
2. Player answers
3. Correct/incorrect feedback plays
4. Shape folds back inward and exits
5. Next round shape enters only after previous fold-out completes

Motion rule:
- Never crossfade two puzzle sets on top of each other; sequence must read clearly

---

## 4.6 Fixed-height feedback region

Problem: correctness text currently shifts other UI when message length/state changes.

Requirement:
- Reserve a fixed-height feedback slot in gameplay HUD
- Always occupy the same vertical space, even when empty
- Messages animate opacity/scale inside the slot, not parent layout height

Examples of messages:
- "Correct"
- "Great streak"
- "Wrong - game over"
- "Delta tightened"

---

## 5) Implementation Phases for Another LLM

## Phase 1 - Tokens and configuration
- Introduce centralized config for:
  - level thresholds
  - level colors
  - gem earn/spend constants
  - ad caps
- Keep all values in one location to support tuning without code churn

## Phase 2 - Brand and splash
- Implement lockup rendering (`Hue` solid + `Zoo` outline)
- Implement splash flicker and color cycle for three levels
- Add reduced-motion fallback

## Phase 3 - Level system integration
- Compute player level from total correct answers
- Bind level color/state to top bar lockup
- Add top bar tap -> level explanation bottom sheet

## Phase 4 - Economy and monetization rules
- Update gem earn on correct answers
- Add refill purchase: 300 gems -> 10 questions
- Add rewarded ad path and interstitial cap logic
- Update blocked/out-of-tries surfaces with clear options

## Phase 5 - Shape variants and transition choreography
- Add flower layout first
- Add one backup layout (orbit or diamond cluster)
- Implement unfold -> play -> fold pipeline with no overlap

## Phase 6 - Layout stability and polish
- Add fixed-height feedback slot
- Verify no vertical jumps on all gameplay states

## Phase 7 - Telemetry + tests
- Add events and test coverage before feature flag removal

---

## 6) Acceptance Criteria

- Brand lockup is consistently rendered as `Hue` solid and `Zoo` outline in top bar and splash
- Splash shows 3 level colors in sequence with flicker effect
- Top bar tap opens level info bottom sheet
- Level progression works for exactly 3 levels with configurable thresholds
- Level 3 entry threshold can be set to `50,000+` without additional code changes
- Player earns at least +1 gem for every correct answer
- Player can spend `300 gems` for `10 questions` when out of tries
- Ad behavior respects caps and never interrupts mid-round gameplay
- At least one non-row shape layout is live with unfold/fold transitions
- Feedback text area is fixed-height; no observable layout shift between states

---

## 7) Telemetry Plan

Track at minimum:
- `splash_started`, `splash_completed`, `splash_skipped`
- `level_changed` (from, to, totalCorrect)
- `level_info_opened`
- `gem_earned` (source, amount)
- `gem_spent` (sink, amount)
- `refill_purchased` (currency=gems, cost=300, reward=10_questions)
- `ad_shown` (type=rewarded|interstitial, placement)
- `ad_reward_granted` (reward type and amount)
- `shape_variant_seen` (flower/orbit/diamond)
- `round_transition_completed` (unfold_ms, fold_ms)

Use telemetry to tune thresholds and economy after launch.

---

## 8) Test Plan (minimum)

Add/extend tests in `commonTest`:
- Level mapping tests: boundary checks around each threshold
- Gem economy tests:
  - +1 per correct
  - refill purchase debits 300 and grants 10 questions
  - insufficient gems rejects purchase
- Ad gate tests:
  - reward granted only after eligible completion callback
  - interstitial frequency cap respected
- UI state tests:
  - top bar level color changes by state
  - bottom sheet open/close state contract
  - feedback slot remains fixed-height across message states

---

## 9) Brainstorming Decisions Needed (with recommendations)

1. Threshold pacing
- Recommended start: Proposal A (`0-199`, `200-49,999`, `50,000+`)
- Fallback if too grindy: Proposal B (`0-499`, `500-9,999`, `10,000+`)

2. Gem pressure
- Recommended: keep `300 -> 10 questions`, but allow one daily discounted refill (`200 -> 10`) to reduce churn

3. Ad mix
- Recommended: rewarded-first economy; interstitials capped and never between rounds

4. Shape rollout
- Recommended: ship flower first, keep others behind feature flags

5. Copy tone
- Recommended: short, game-like, high-energy microcopy (no verbose educational text in round HUD)

---

## 10) Suggested Work Packet Format for Another LLM

When assigning implementation, split prompts into small packets:
1. Config + models
2. Splash + lockup visuals
3. Level HUD + bottom sheet
4. Economy rules + ads
5. Shape layout + transitions
6. Tests + telemetry

Each packet must include:
- files to modify
- state contract changes
- acceptance checklist
- explicit test cases

---

## 11) Proposed Initial Constants (editable)

```text
LEVEL_1_MAX_CORRECT = 199
LEVEL_2_MAX_CORRECT = 49_999
LEVEL_3_MIN_CORRECT = 50_000

GEMS_PER_CORRECT = 1
GEMS_FIRST_WIN_DAILY = 20
GEMS_STREAK_5 = 5
GEMS_STREAK_10 = 15

REFILL_GEM_COST = 300
REFILL_QUESTION_REWARD = 10

INTERSTITIAL_SESSION_CAP = 1
INTERSTITIAL_EVERY_N_SESSIONS = 3
```

Keep these centralized and remotely tunable later if possible.

