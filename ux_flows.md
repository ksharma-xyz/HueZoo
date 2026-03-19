# Huezoo UX Flows
_Delete this file once all flows are coded and verified._

---

## Backstack shape reference
```
[Home]
[Home, ThresholdGame]      ← during game
[Home, Result]             ← after game ends (ThresholdGame removed before Result pushed)
[Home]                     ← after Result back / Play Again daily
[Home, ThresholdGame]      ← Play Again threshold (fresh ViewModel)
```

---

## Flow 1 — Launch
| Step | Action | Result | Handled? |
|---|---|---|---|
| App open | — | Home screen shown, HomeViewModel loads card data | ✅ |
| Threshold attempts exhausted | — | Card shows "No tries left", disabled | ✅ |
| Daily already played | — | Card shows "Done" badge, disabled | ✅ |

**Gap:** HomeScreen `LaunchedEffect(Unit)` only fires once. Returning from a game does NOT refresh card state (tries used, daily done). Cards may show stale data.
**Fix needed:** Call `HomeUiEvent.ScreenResumed` on lifecycle RESUME, not just on first composition.

---

## Flow 2 — Threshold game
| Step | Action | Result | Handled? |
|---|---|---|---|
| Tap Threshold card | → push ThresholdGame | Fresh screen + VM, `loadGame()` runs | ✅ |
| Attempt available | — | Playing state: 3 swatches + ΔE badge | ✅ |
| Attempts exhausted | — | Blocked state: "No tries left" + reset time | ✅ |
| Tap correct swatch | — | Swatch flashes green → next round (harder ΔE) | ✅ |
| Tap wrong swatch | — | Wrong swatch red, outlier revealed → Result | ✅ (fixed) |
| Tap Back mid-game | → pop ThresholdGame | Home shown | ✅ |
| Come back and tap card again | → push ThresholdGame | Fresh VM, `init` re-runs | ✅ (fixed) |

**Fixed bug:** Previously, game screen stayed on backstack below Result. Backing from Result resurfaces the stuck VM (`roundPhase = Wrong`) — all taps silently ignored. Now game screen is removed before Result is pushed.

---

## Flow 3 — Result screen (Threshold)
| Step | Action | Result | Handled? |
|---|---|---|---|
| Game ends (wrong tap) | → pop ThresholdGame, push Result | Result shown | ✅ (fixed) |
| Tap "Play Again" | → pop Result, push ThresholdGame | Fresh VM, new game | ✅ (fixed) |
| Tap "Back" | → pop Result | Home shown | ✅ |
| Tap "Leaderboard" | → push Leaderboard | Leaderboard stub | ✅ (stub) |

---

## Flow 4 — Daily game
| Step | Action | Result | Handled? |
|---|---|---|---|
| Tap Daily card | → push DailyGame | Fresh screen + VM | ✅ |
| Play all rounds | — | After last round → Result | ✅ |
| Tap "Play Again" | → pop Result, do NOT push Daily | Home shown (once-per-day) | ✅ (fixed) |
| Tap "Back" from game mid-play | → pop DailyGame | Home shown | ✅ |
| Return to Home after daily done | — | Daily card should show "Done" | ❌ stale — see Flow 1 gap |

---

## Flow 5 — Leaderboard (stub)
| Step | Handled? |
|---|---|
| Tap Leaderboard from Result | ✅ navigates to stub screen |
| Back from Leaderboard | ✅ |

---

## Open gaps (fix before shipping)

| # | Gap | Where to fix |
|---|---|---|
| G1 | Home card data stale on return — `ScreenResumed` not re-fired | `HomeScreen.kt` — use lifecycle-aware resume event |
| G2 | Result "Play Again" button says "Play Again" even for Daily (should say "Back to Home") | `ResultScreen.kt` — show different label based on `gameId` |
| G3 | No visual confirmation when daily is already played mid-session | `DailyScreen` `AlreadyPlayed` state — needs proper UI (not just text) |
| G4 | Leaderboard is a stub | Future phase |
| G5 | Paywall not triggered | Future phase |
