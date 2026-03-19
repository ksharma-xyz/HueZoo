# Testing TODO

All tests to be written after core implementation is complete.

## Domain Layer

### `DefaultThresholdGameEngine`
- `generateRound()` returns a `GameRound` where exactly one swatch is the oddColor
- `generateRound()` places the odd swatch at a random index (0, 1, or 2)
- `generateRound()` fills remaining indices with `baseColor`
- `oddIndex` in returned `GameRound` matches the actual odd swatch position

### `DefaultDailyGameEngine`
- `totalRounds` equals 6
- `deltaECurve` has 6 elements in descending order
- `generateRound()` is deterministic: same date + roundIndex always produces same `oddIndex`
- `generateRound()` produces different `oddIndex` for different `roundIndex` values (same date)
- `generateRound()` produces different `oddIndex` for different dates (same `roundIndex`)
- `oddIndex` in returned `GameRound` matches the actual odd swatch position

### `ColorEngine` (existing, verify coverage)
- `generateOddSwatch()` produces a color that is `deltaE` perceptually different from `baseColor`
- `generateOddSwatch()` stays within gamut (RGB channels clamped 0–1)

---

## Data Layer

### `DefaultThresholdRepository`

#### `getAttemptStatus()`
- Returns `Available(attemptsUsed=0, maxAttempts=5)` when no session exists
- Returns `Available(attemptsUsed=N)` when session has N attempts used (N < MAX_ATTEMPTS)
- Returns `Exhausted(nextResetAt)` when `attempts_used >= MAX_ATTEMPTS`
- Deletes expired sessions before checking (expired session treated as "no session")

#### `recordAttempt()`
- Creates a new session with `attempts_used=1` when no active session exists
- Increments `attempts_used` when active session already exists
- New session `next_reset_at` is `now + 8 hours`

#### `getPersonalBest()`
- Returns `null` when no personal best recorded
- Returns `PersonalBest` with correct fields when one exists

#### `savePersonalBest()`
- Saves when no previous best exists
- Overwrites when new `deltaE` is strictly less than current best
- Does NOT overwrite when new `deltaE` is greater than or equal to current best

### `DefaultDailyRepository`

#### `getTodayChallenge()`
- Returns `null` when today's date has no record
- Returns `DailyChallenge` with correct `date`, `score`, `completed` when record exists

#### `saveChallenge()`
- Persists a new record for the given date
- Idempotent on re-save (upsert semantics)

### `DefaultSettingsRepository`

#### `isPaid()`
- Returns `false` when key not set
- Returns `true` after `setPaid(true)`
- Returns `false` after `setPaid(false)`

#### `setPaid()`
- Persists value across subsequent `isPaid()` calls

---

## ViewModel Layer

### `ThresholdViewModel`

#### Initial state
- Emits `Loading`, then transitions to `Blocked` when attempt gate is exhausted
- Emits `Loading`, then transitions to `Playing` when attempts remain

#### `SwatchTapped` — correct guess
- State transitions to `RoundPhase.Correct` immediately
- After delay (~350 ms), advances to next round with new swatches
- After final correct round, emits `NavigateToResult` nav event with correct `Result`

#### `SwatchTapped` — wrong guess
- State transitions to `RoundPhase.Wrong` immediately
- After delay (~450 ms), emits `NavigateToResult` nav event
- Score is 0 if wrong on round 1 (no `lastSuccessfulDeltaE`)
- Score uses `lastSuccessfulDeltaE`, not failing `deltaE`

#### `roundsSurvived`
- Equals `roundCount - 1` (rounds completed before failure)

### `DailyViewModel`

#### Already played
- Emits `AlreadyPlayed` with persisted score when today's challenge record exists

#### Playing
- Emits `Playing` state with correct `totalRounds` and initial `round = 0`
- Correct tap on non-final round advances to next round
- Correct tap on final round calls `finishGame()` and emits nav event
- Wrong tap on any round calls `finishGame()` and emits nav event

#### `finishGame()`
- Saves challenge via `DailyRepository.saveChallenge()`
- Emits `NavigateToResult` with accumulated `cumulativeScore`

### `HomeViewModel`

#### Initial load
- Emits `Ready` with correct `attemptsRemaining`, `isBlocked`, `personalBestDeltaE`, `isCompletedToday`, `isPaid`
- `ScreenResumed` event triggers a full reload of all card data

#### Navigation events
- `ThresholdCardTapped` emits `NavigateToThreshold`
- `DailyCardTapped` emits `NavigateToDaily`
- `UnlockTapped` emits `NavigateToPaywall`

### `ResultViewModel`
- Emits `Loading`, then `Ready` with correct `isNewPersonalBest = true` when score beats existing PB
- Emits `isNewPersonalBest = false` when score does not beat existing PB
- Emits `isNewPersonalBest = true` when no prior PB exists

---

## Notes
- Use a fake/in-memory database (or SQLDelight in-memory driver) for repository tests — avoid mocking the DB layer
- Inject `Random` via constructor in `DefaultThresholdGameEngine` to control randomness in tests
- Use `TestCoroutineScheduler` / `UnconfinedTestDispatcher` for ViewModel timing tests
- Verify navigation events via `SharedFlow` collection in a `launch` block before triggering events
