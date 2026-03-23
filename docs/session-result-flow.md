# Session Result Flow

How game session data travels from a game ViewModel to the Result screen.

## Problem This Solves

The naive approach — packing session data into nav args and reading them back in `ResultViewModel` via Koin parameters — breaks silently:

1. `koinViewModel(parameters = { parametersOf(result) })` only passes parameters when the ViewModel is **first created**. Koin caches ViewModels in the Activity's `ViewModelStore` by class name. On the second "play again" session, `koinViewModel` finds the existing `ResultViewModel` and returns it unchanged — the new `result` parameters are ignored.
2. The Result screen therefore renders the **first session's data** for every subsequent session.
3. Adding a `key=` to `koinViewModel` patches the symptom but is fragile (key uniqueness must be manually maintained, and leaks old ViewModels in the store).

## Solution: `SessionResultCache`

A singleton `StateFlow`-based cache that game ViewModels write into just before navigating. `ResultViewModel` collects from it reactively.

```
ThresholdViewModel / DailyViewModel
  │
  ├─ saves personal best to DB (NonCancellable, survives back press)
  ├─ sessionResultCache.set(SessionResult(...))   ← write to cache
  └─ _navEvent.emit(NavigateToResult)             ← signal only, no data

      ↓  navigation happens

ResultViewModel (may be new OR reused instance)
  │
  └─ init { cache.result.filterNotNull().collect { load(it) } }
       └─ load() reads DB for personal best + canPlayAgain
            └─ emits ResultUiState.Ready
```

## Data Classes

### `SessionResult` (`navigation/SessionResult.kt`)

In-memory only — **not** `@Serializable`. Holds all game result data:

```kotlin
data class SessionResult(
    val gameId: String,
    val deltaE: Float,
    val roundsSurvived: Int,
    val correctRounds: Int,
    val totalRounds: Int,
    val gemsEarned: Int,
    val gemBreakdown: List<GemAward>,
)
```

### `SessionResultCache` (`domain/game/SessionResultCache.kt`)

Singleton registered as `single { SessionResultCache() }` in `AppModule`.

```kotlin
class SessionResultCache {
    private val _result = MutableStateFlow<SessionResult?>(null)
    val result: StateFlow<SessionResult?> = _result.asStateFlow()

    fun set(sessionResult: SessionResult) { _result.value = sessionResult }
}
```

The cache is **never cleared**. It always holds the most recent session result. This is intentional: if the user navigates back and forward to the Result screen, they see the latest result. Since the back stack always starts fresh at `Splash` on process death, there is no stale-cache scenario.

## Why `filterNotNull().collect` in `ResultViewModel`

```kotlin
viewModelScope.launch {
    sessionResultCache.result.filterNotNull().collect { result ->
        load(result)
    }
}
```

- `filterNotNull()` means: do nothing until a result is available. The screen stays in `Loading` state until the cache is written.
- `collect` (not `collectLatest`) means: each new value triggers a full `load()`. This is intentional — each session is a complete reload including fresh DB reads for personal best and attempt status.
- Because the ViewModel **collects reactively**, it handles its own reuse. Even if Koin returns the same `ResultViewModel` instance for a second session, the new cache value triggers a new `load()` automatically.

## Sequence for a Full "Play Again" Cycle

```
1. User plays ThresholdGame session 1
2. ThresholdViewModel.handleWrongTap (last try):
   a. repository.savePersonalBest(finalDeltaE)   ← DB updated
   b. sessionResultCache.set(SessionResult(...))  ← cache updated
   c. _navEvent.emit(NavigateToResult)            ← nav signal
3. ThresholdScreen collects event → calls onResult()
4. App.kt: backStack removes ThresholdGame, adds Result("threshold")
5. ResultScreen enters composition
6. ResultViewModel.init starts collecting cache
7. cache.result emits SessionResult for session 1 → load() called
8. ResultUiState.Ready emitted → UI renders

9. User taps "PLAY AGAIN"
10. App.kt: backStack removes Result, adds ThresholdGame
11. User plays session 2
12. ThresholdViewModel repeats steps 2a-2c with new session data
13. App.kt: backStack removes ThresholdGame, adds Result("threshold")
14. ResultScreen enters composition (may reuse same ResultViewModel instance)
15. cache.result emits new SessionResult for session 2 → load() called again
16. ResultUiState.Ready emitted with fresh data ✓
```

## Personal Best: DB vs Cache

Personal best is **never stored in the cache**. The flow is:

1. `ThresholdViewModel.handleCorrectTap`: calls `repository.savePersonalBest(sessionBest)` immediately (wrapped in `NonCancellable`) when a new all-time best is detected. DB is the source of truth from the moment it's set.
2. `ResultViewModel.load()`: calls `repository.getPersonalBest()` fresh from DB. This guarantees the displayed "ALL-TIME BEST ΔE" reflects the DB state, not any in-memory approximation.
3. `isNewPersonalBest` detection: compares `DB best ≈ session deltaE` (within 0.005f tolerance for float precision). Works because `savePersonalBest` runs before `set()` and before the nav event.

## Previous Bug: `isReachPersonalBest`

A removed bug worth documenting to prevent reintroduction.

`ThresholdViewModel.handleWrongTap` previously contained:

```kotlin
// REMOVED — was incorrectly saving the failed ΔE level as personal best
val isReachPersonalBest = bestDeltaE != null &&
    (storedBestDeltaE == null || currentDeltaE < storedBestDeltaE!!)
if (isReachPersonalBest) {
    repository.savePersonalBest(currentDeltaE)  // BUG: currentDeltaE is the FAILED level
}
```

**Why it was wrong**: `currentDeltaE` on a wrong tap is the level the player _attempted but failed_. Saving it as personal best meant:
- The DB stored a ΔE the player never successfully identified.
- `isNewPersonalBest` detection broke: `navResult.deltaE` (the last _correctly_ identified level) and `DB best` (the failed level) diverged by more than 0.005f, so `isNewPersonalBest = false` even when the player genuinely improved.
- The "ALL-TIME BEST ΔE" stat card on the result screen showed an artificially better value never actually achieved.

**The fix**: personal best is only saved on _correct_ taps (in `handleCorrectTap`) and at session end via `finalDeltaE = bestDeltaE ?: currentDeltaE`.
