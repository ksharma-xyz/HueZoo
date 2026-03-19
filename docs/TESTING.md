# Huezoo — Testing Architecture

> **Rule:** Every interface has a fake. Every fake is in `commonTest`. Every ViewModel test uses fakes, never real implementations via Koin.

---

## Philosophy

The app is split into three testability tiers:

```
Tier 1 — Pure math (zero dependencies)
  ColorMath.kt — sRGB↔Lab, CIEDE2000
  → Test directly. No mocking needed. Use fixed inputs, assert fixed outputs.

Tier 2 — Game logic (random + date, injectable)
  DefaultColorEngine — randomVividColor, generateOddSwatch, seededColorForDate
  → Test with seeded Random. Inject via ColorEngine interface.

Tier 3 — UI / ViewModels (compose state, DB, network)
  ThresholdViewModel, DailyViewModel, etc.
  → Inject FakeColorEngine + fake DB. Never use real Koin modules.
```

---

## The Interface Rule

**Every class that has side effects, uses random, or touches external systems must be defined as an interface.** The concrete implementation is always named `Default*`.

```
ColorEngine (interface)          ← what ViewModels inject
  └── DefaultColorEngine         ← production, in Koin as single<ColorEngine>
  └── FakeColorEngine            ← in commonTest, fixed outputs
```

This pattern will apply to every new layer as the project grows:

| Future interface | Side effect it hides | Fake strategy |
|---|---|---|
| `ColorEngine` | Random color generation | `FakeColorEngine` — fixed colors |
| `ThresholdRepository` | SQLDelight reads/writes | `FakeThresholdRepository` — in-memory |
| `DailyRepository` | Date-based DB + seeded color | `FakeDailyRepository` — fixed state |
| `LeaderboardRepository` | Firebase network calls | `FakeLeaderboardRepository` — fixed list |
| `HapticEngine` | Platform haptics | `NoOpHapticEngine` — does nothing |
| `SoundPlayer` | Platform audio | `NoOpSoundPlayer` — does nothing |

---

## Running Tests

```bash
# Run all unit tests (Android JVM — no device needed)
./gradlew :composeApp:testDebugUnitTest

# Run with HTML report
./gradlew :composeApp:testDebugUnitTest --info

# Run a single test class
./gradlew :composeApp:testDebugUnitTest --tests "*.ColorMathTest"
./gradlew :composeApp:testDebugUnitTest --tests "*.ColorEngineTest"
```

Tests live in `commonTest` and run as Android JVM unit tests (no emulator needed).

---

## Test File Locations

```
composeApp/src/
├── commonMain/kotlin/xyz/ksharma/huezoo/
│   └── domain/color/
│       ├── ColorEngine.kt          ← interface (inject this)
│       ├── DefaultColorEngine.kt   ← implementation (never import in tests)
│       ├── ColorMath.kt            ← pure top-level math functions
│       ├── Lab.kt                  ← CIELAB data class
│       └── di/ColorModule.kt       ← Koin single<ColorEngine> { DefaultColorEngine() }
│
└── commonTest/kotlin/xyz/ksharma/huezoo/
    └── domain/color/
        ├── FakeColorEngine.kt      ← test double — fixed return values
        ├── ColorMathTest.kt        ← tests for pure math (rgbToLab, deltaE, round-trip)
        └── ColorEngineTest.kt      ← tests for game API (randomVividColor, generateOddSwatch, ...)
```

---

## Seeded Randomness Pattern

Functions that use randomness take `random: Random = Random.Default` as a parameter. Tests always pass `Random(fixedSeed)`:

```kotlin
// Production — uses system random (non-deterministic)
val engine = DefaultColorEngine()   // Random.Default

// Tests — always deterministic
val engine = DefaultColorEngine(random = Random(12345L))
```

**Never** use `Random.Default` in a test — it makes tests flaky by definition.

---

## FakeColorEngine Usage

```kotlin
// In a ViewModel test:
val fake = FakeColorEngine(
    vividColor = Color.Red,       // randomVividColor() always returns Red
    oddSwatch  = Color.Blue,      // generateOddSwatch(...) always returns Blue
    dailyColor = Color(0xFFFFE600), // seededColorForDate(...) always returns Yellow
    score      = 750,             // scoreFromDeltaE(...) always returns 750
)
val viewModel = ThresholdViewModel(colorEngine = fake, ...)
// Now test state transitions with fully predictable colour values
```

---

## What NOT to Do

```
❌ NEVER use Koin in unit tests
✅ Construct DefaultColorEngine(random = Random(seed)) directly

❌ NEVER use Random.Default in a test
✅ Always pass Random(fixedSeed) for deterministic results

❌ NEVER test UI rendering in unit tests (no @Preview, no Compose test)
✅ Test only state: what goes into the ViewModel, what comes out

❌ NEVER mock ColorMath functions — they are pure and always correct
✅ Just call them directly in tests that need Lab/ΔE values

❌ NEVER import DefaultColorEngine in a ViewModel
✅ Always inject ColorEngine (the interface) via Koin constructor injection
```

---

## Tolerances

| What | Tolerance | Reason |
|---|---|---|
| Lab component values | ±0.5 | Float precision through gamma + matrix math |
| ΔE (CIEDE2000) | ±0.01 | Double-precision arithmetic, game only needs ±0.1 |
| Round-trip RGB | ±0.01 per channel | Acceptable float error through Lab pipeline |
| Odd swatch ΔE | ±0.1 | Binary search precision is ~0.00004; clamping adds small error |

---

## Phase-by-Phase Testing Plan

| Phase | What to test | Fake needed |
|---|---|---|
| 1 ✅ | ColorMath (pure), ColorEngine (seeded) | FakeColorEngine |
| 2 | SwatchBlock game state transitions | FakeColorEngine |
| 3 | HomeScreen ViewModel — card data from DB | FakeThresholdRepository |
| 4 | ThresholdViewModel — round logic, attempt gate | FakeColorEngine + FakeThresholdRepository |
| 5 | DailyViewModel — seeded puzzle, replay block | FakeColorEngine + FakeDailyRepository |
| 6 | ResultViewModel — score display, share text | FakeColorEngine |
| 7 | Paywall — attempt exhaustion triggers sheet | FakeThresholdRepository |
| 8 | LeaderboardViewModel — rank list rendering | FakeLeaderboardRepository |
