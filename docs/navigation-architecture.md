# Navigation Architecture

## Library

Huezoo uses **AndroidX Navigation 3** (`androidx.navigation3`) with a custom back stack managed as a plain Kotlin `MutableList<Any>`. This is a pre-1.0 alpha library — key implication: _do not assume Jetpack Navigation 2 patterns apply here_.

## Back Stack

```kotlin
val backStack = remember { mutableStateListOf<Any>(Splash) }
```

- Each element is a **destination object** (e.g., `Home`, `ThresholdGame`, `Result("threshold")`).
- The list is owned by `App.kt` and passed directly to `NavDisplay`.
- Navigation = list mutation: `backStack.add(dest)`, `backStack.removeLast()`, `backStack[lastIndex] = dest`.
- There is **no process-death restoration** — the list is in-memory only. On process death/kill, the app always restarts at `Splash`.

## Destinations

All destinations live in `navigation/AppDestinations.kt` and are `@Serializable` data objects/classes (required by Navigation 3 for route encoding):

| Destination | Type | Note |
|---|---|---|
| `Splash` | `data object` | Entry point; replaces itself in-place |
| `EyeStrainNotice` | `data object` | Replaces itself in-place |
| `Home` | `data object` | Root after onboarding |
| `ThresholdGame` | `data object` | Threshold game screen |
| `DailyGame` | `data object` | Daily challenge screen |
| `Result(gameId)` | `data class` | Result screen — carries only `gameId` for identity/color, **not** game data |
| `Leaderboard` | `data object` | |
| `Settings` | `data object` | |
| `Upgrade` | `data object` | |

### `Result` destination design note
`Result` intentionally carries **only `gameId`** (not scores, gems, deltaE, etc.). All session result data travels through `SessionResultCache` — see `session-result-flow.md`. This avoids nav-arg serialization bugs and the Koin ViewModel reuse issue that arises when parameters are silently ignored on retrieval.

## `NavDisplay` and ViewModel Scoping

`NavDisplay` matches each `backStack` element against the `entryProvider` lambda and renders the matching `NavEntry`. Each `NavEntry(key) { ... }` gets its own composition scope keyed by the destination object.

**Known limitation**: Navigation 3 is alpha. Per-`NavEntry` ViewModel scoping via `LocalViewModelStoreOwner` may or may not be fully reliable depending on the version in use. The `SessionResultCache` singleton pattern (rather than nav-arg-based ViewModel parameters) is the defensive design choice that sidesteps this entirely — `ResultViewModel` reads from the cache reactively and never depends on constructor parameters that could be stale on reuse.

## Back Gesture / System Back

```kotlin
onBack = { if (backStack.size > 1) backStack.removeLast() }
```

Passed to `NavDisplay`. Prevents the back stack from going empty. Individual screens can override `onBack` behaviour (e.g., `Splash` and `EyeStrainNotice` replace in-place instead of popping).

## Game → Result Navigation Pattern

Game screens (Threshold, Daily) follow this pattern:

```
[Home, ThresholdGame]
  └─ game ends
[Home]               ← ThresholdGame removed
  └─ Result added
[Home, Result("threshold")]
  └─ "Play Again"
[Home]               ← Result removed
  └─ ThresholdGame added
[Home, ThresholdGame]
```

The game screen is **removed before** Result is added so the back stack stays `[Home, Result]` rather than `[Home, Game, Result]`. Pressing Back from Result returns directly to Home, not back to a stale game screen.

## Navigation Events (NavEvents)

Game ViewModels emit one-shot navigation commands via `MutableSharedFlow(extraBufferCapacity = 1)`. The screen collects these in a `LaunchedEffect(Unit)`:

```kotlin
LaunchedEffect(Unit) {
    viewModel.onStart()
    viewModel.navEvent.collect { event ->
        when (event) {
            ThresholdNavEvent.NavigateToResult -> onResult()
            ThresholdNavEvent.NavigateBack -> onBack()
        }
    }
}
```

`extraBufferCapacity = 1` ensures the event isn't dropped if the collector isn't ready yet (e.g., mid-recomposition).

Nav events carry **no data** — they are pure signals. All result data is written to `SessionResultCache` before the event is emitted.

## What NOT to Pass Through Nav Args

Never pass mutable game state through nav args. Problems encountered:
- Koin's `koinViewModel(parameters = {...})` only applies parameters on _first creation_. If the Activity-scoped ViewModel already exists (from a previous navigation to the same destination), the new parameters are silently ignored and the old ViewModel is returned.
- The `@Serializable` constraint forces all nav arg types to be serializable, adding friction for complex domain types.

Use `SessionResultCache` or equivalent singletons for any data that needs to survive a navigation boundary.
