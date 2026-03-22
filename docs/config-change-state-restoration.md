# Configuration Change & State Restoration Analysis

## The Problem

On Android, a **configuration change** (screen rotation, font-size adjustment, split-screen, locale change, etc.) causes the Activity to be **destroyed and recreated**. Unless state is explicitly persisted, every composable recomposes from scratch — and in Huezoo that means the user lands back on the splash screen every time they rotate their phone or bump the system font size.

---

## Root Cause: `remember` vs `rememberSaveable`

### The broken line — `App.kt:48`

```kotlin
val backStack = remember { mutableStateListOf<Any>(Splash) }
```

`remember {}` survives **recomposition** but NOT **Activity recreation**. When Android destroys and recreates the Activity:

1. `App()` is called fresh.
2. `remember {}` re-executes → `backStack = [Splash]`.
3. `NavDisplay` shows `SplashScreen`.
4. Splash animation plays for ~1.2 s, then `settingsRepository.hasSeenHealthNotice()` is checked.
5. User is navigated to `Home` — but any deeper destination (game in progress, result screen, settings) is **gone**.

`rememberSaveable {}` persists through Activity recreation by writing to the `SavedStateRegistry` (the same Bundle mechanism Android has always used). Navigation 3 provides a purpose-built helper for back-stack survival: **`rememberNavBackStack()`**.

---

## What KRAIL Does Differently

KRAIL uses `rememberNavBackStack()` from Navigation 3 (`androidx.navigation3`). This function is `rememberSaveable` under the hood — it serialises each route to the `SavedStateRegistry` using the route's `@Serializable` annotation, so the entire back stack (including route parameters) survives rotation.

```kotlin
// KRAIL — NavigationState.kt
val backStacks = topLevelRoutes.associateWith { key ->
    rememberNavBackStack(serializationConfig, key)   // survives config changes
}
```

It also wraps each `NavEntry` with `rememberSaveableStateHolderNavEntryDecorator`, which ensures every `rememberSaveable {}` call **inside** a screen composable is also restored correctly when the screen reappears after rotation.

```kotlin
val decorators = listOf(
    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
)
val entries = rememberDecoratedNavEntries(
    backStack = stack,
    entryDecorators = decorators,
    entryProvider = entryProvider,
)
```

---

## All Issues in Huezoo & Required Fixes

### 1. Back stack not persisted across config changes

| | |
|---|---|
| **File** | `App.kt:48` |
| **Now** | `val backStack = remember { mutableStateListOf<Any>(Splash) }` |
| **Fix** | `val backStack = rememberNavBackStack(Splash)` |

`rememberNavBackStack` is already in the `androidx.navigation3:navigation3-runtime` dependency Huezoo uses (it's how `NavDisplay` works). All routes are already `@Serializable` (`AppDestinations.kt`) so no route changes are needed.

`rememberNavBackStack` takes a `SavedStateConfiguration` that registers a polymorphic serializer for the sealed hierarchy. A config like KRAIL's `krailNavSerializationConfig` needs to be added in Huezoo that lists every destination subtype.

```kotlin
// to add: e.g. HuezooNavSerializationConfig.kt
val huezooNavSerializationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(Any::class) {
            subclass(Splash::class, Splash.serializer())
            subclass(EyeStrainNotice::class, EyeStrainNotice.serializer())
            subclass(Home::class, Home.serializer())
            subclass(ThresholdGame::class, ThresholdGame.serializer())
            subclass(DailyGame::class, DailyGame.serializer())
            subclass(Result::class, Result.serializer())
            subclass(Leaderboard::class, Leaderboard.serializer())
            subclass(Settings::class, Settings.serializer())
        }
    }
}

// App.kt — replace line 48
val backStack = rememberNavBackStack(Splash, huezooNavSerializationConfig)
```

---

### 2. `NavEntry` composable state not restored

| | |
|---|---|
| **File** | `App.kt:64` — `entryProvider = { … }` |
| **Now** | Raw `entryProvider` lambda, no decorators |
| **Fix** | Wrap entries with `rememberSaveableStateHolderNavEntryDecorator` |

Without this decorator, every `rememberSaveable {}` call inside a screen composable is **orphaned** on config change — its saved value exists in the registry but is never re-associated with the recomposed screen.

```kotlin
// App.kt — replace NavDisplay call
val entryProvider = entryProvider { destination ->
    when (destination) { /* same as now */ }
}

val decoratedEntries = rememberDecoratedNavEntries(
    backStack = backStack,
    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
    entryProvider = entryProvider,
)

NavDisplay(
    entries = decoratedEntries,
    onBack = { if (backStack.size > 1) backStack.removeLast() },
)
```

---

### 3. Local screen state uses `remember` instead of `rememberSaveable`

Any UI state that should survive config changes (sheet visibility, dialog open/closed, scroll position, animation-already-played flags) must use `rememberSaveable`.

| File | State variable | Fix |
|------|---------------|-----|
| `HomeScreen.kt:161-162` | `showLevelsSheet`, `showPerceptionSheet` | `rememberSaveable { mutableStateOf(false) }` |
| `ThresholdScreen.kt:62` | `showHelp` | `rememberSaveable { mutableStateOf(false) }` |
| `DailyScreen.kt:67` | similar dialog/sheet flags | `rememberSaveable { mutableStateOf(false) }` |
| `SplashScreen.kt:63-66` | `zooFilled`, `zooGlowAlpha`, `taglineAlpha`, `screenAlpha` | Not needed — splash should always re-animate on cold start; these are fine as `remember` |

For `Boolean` / `Int` / `String` / `Float` types, `rememberSaveable` works with zero boilerplate — they are Parcelable by default via the Bundle codec.

---

### 4. `gems` counter in `App.kt` resets to 0 on rotation

| | |
|---|---|
| **File** | `App.kt:53` |
| **Now** | `var gems by remember { mutableIntStateOf(0) }` |
| **Fix** | `var gems by rememberSaveable { mutableIntStateOf(0) }` |

This avoids a brief flash where the player level (and therefore accent color) shows the default before `LaunchedEffect` re-reads it from the repository.

---

### 5. ViewModels are scoped correctly — no change needed

Huezoo already uses `koinViewModel()` inside each `NavEntry`. Navigation 3 scopes `ViewModel` instances to the **`NavEntry`** (backed by its own `ViewModelStoreOwner`), not the Activity. This means:

- As long as the `NavEntry` stays in the back stack (which it will, once fix #1 is applied), the `ViewModel` is **not destroyed on config change**.
- `StateFlow` / `MutableStateFlow` in ViewModels continues collecting from where it left off.
- `LifecycleResumeEffect` and `collectAsStateWithLifecycle` both hook into the entry's lifecycle, not the Activity's — already correct.

The only ViewModel that's a special case is `ResultViewModel`, which takes `Result` as a constructor parameter. Once the back stack is restored after rotation, Navigation 3 will reconstruct the `NavEntry` with the same `Result` object (since `Result` is `@Serializable` and is stored in the restored back stack) — so the ViewModel will be re-created with the correct parameters.

---

## Summary of Changes

| Priority | Change | File | Effort |
|----------|--------|------|--------|
| **Critical** | Replace `remember {}` back stack with `rememberNavBackStack()` + serialization config | `App.kt`, new `HuezooNavSerializationConfig.kt` | Small |
| **Critical** | Add `rememberSaveableStateHolderNavEntryDecorator` to `NavDisplay` | `App.kt` | Small |
| **Medium** | `rememberSaveable` for `gems` in App | `App.kt:53` | Trivial |
| **Medium** | `rememberSaveable` for sheet/dialog flags in screens | `HomeScreen`, `ThresholdScreen`, `DailyScreen` | Trivial per file |
| **None** | ViewModel scoping | — | No change needed |
| **None** | Route `@Serializable` annotations | `AppDestinations.kt` | Already done |

---

## Why No `android:configChanges` Hack

A common shortcut is to add `android:configChanges="orientation|screenSize|..."` to `AndroidManifest.xml` so the Activity is never destroyed. This is **not recommended** because:

1. It only covers the config changes you list — any unlisted change (e.g. locale, density, font-scale) still destroys the Activity.
2. It can break Material window insets, adaptive layout re-measurement, and accessibility features that depend on Activity recreation.
3. The correct fix (`rememberSaveable` + `rememberNavBackStack`) handles all config changes automatically and is the Jetpack-endorsed approach.

---

## Reference: KRAIL Patterns to Reuse

| Pattern | KRAIL Location |
|---------|---------------|
| `rememberNavBackStack` with serialization config | `core/navigation/src/commonMain/…/NavigationState.kt` |
| `rememberSaveableStateHolderNavEntryDecorator` | `composeApp/…/KrailNavHost.kt` via `toEntries()` |
| Polymorphic serialization config | `composeApp/…/navigation/SerializationConfig.kt` |
| `rememberSaveable` animation-played flag | `feature/…/AppUpgradeScreen.kt` — `AnimatedUpdateButton` |
