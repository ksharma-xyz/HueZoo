# Huezoo — UX Review

*Screen-by-screen issues, gaps, and proposed fixes. Items marked [MVP] are blocking ship quality. Items marked [Polish] are nice-to-have.*

---

## 1. First Launch / Onboarding

**Problems:**
- User is dropped straight into Home with no context
- "ΔE" appears everywhere (badge, result card, home card) but is never explained
- New user doesn't know what "The Threshold" means or how to play

**Fixes:**
- [MVP] One-time "What is ΔE?" info card on Home, dismissable, shown on first launch only. Simple: "ΔE measures color difference. Lower = harder to spot."
- [MVP] Each game card needs a one-line mechanic hint below the subtitle (already have subtitle, just make it more instructional for new users)
- [Polish] Animated first-round tooltip in Threshold: "Tap the odd one out" already shown — good. Keep it.

---

## 2. Home Screen

**Problems:**
- Daily card after completion: shows "Done" badge but no score and no countdown to next puzzle → feels dead
- Threshold card when blocked: shows "No tries left" but no info on when attempts reset → user has no reason to come back
- Loading state = blank screen, jarring

**Fixes:**
- [MVP] Daily card (completed): show today's score + countdown "Next puzzle in Xh Xm"
- [MVP] Threshold card (blocked): show "Resets in Xh Xm" countdown next to the tries text
- [MVP] Loading state: show skeleton/shimmer or at minimum keep cards visible but dimmed while refreshing
- [Polish] Threshold card: show personal best ΔE prominently ("Your best: ΔE 1.4")

---

## 3. The Threshold — Gameplay

**Problems:**
- Correct tap: green flash on swatch, then immediately new round → no positive momentum signal
- ΔE decreases each correct round but user may not understand this is getting harder
- No "you're on a streak" feeling between rounds
- HUD shows ROUND + TRIES but no indication of how difficult the current level is
- When wrong: straight to result with no pause to absorb

**Fixes:**
- [MVP] Correct tap: brief "↓ ΔE X.X" label slides in below the badge for 600ms before next round — makes the progression feel earned
- [MVP] DeltaEBadge color already changes cyan→yellow→magenta by difficulty — good. Add a subtle pulse on each difficulty tier change.
- [Polish] Add a sting text under "Tap the odd one out" that changes by ΔE tier: e.g. "Easy" / "Getting harder" / "Expert level" / "Superhuman"
- [Polish] Wrong tap: 450ms delay before navigating (already in code) is good — let the shake + reveal breathe

---

## 4. Daily Challenge — Gameplay

**Problems:**
- Looks identical to Threshold but plays differently (fixed ΔE curve, 6 rounds always)
- User doesn't know the structure — "how many rounds?" — ROUND chip helps but no total context shown beyond "1/6"
- No sense of a shared experience ("everyone is playing this today")

**Fixes:**
- [MVP] Under "Daily Challenge" title add subtitle with today's date: "March 20 · Same for everyone"
- [MVP] ROUND chip already shows "1/6" — good. Ensure it's clear this is a fixed 6-round game.
- [Polish] On final round (6/6): change "Tap the odd one out" to "Last one — make it count"

---

## 5. Result Screen

**Problems:**
- No celebration moment — result appears but nothing marks it as special
- No share button → biggest missed viral loop
- Generic text on card — no personality, no sting
- Daily result: no countdown to next puzzle
- Threshold result: "Play Again" should check attempt count before showing
- Leaderboard button goes nowhere (stub)

**Fixes:**
- [MVP] Confetti burst on result enter — identity color particles
- [MVP] Share button below the two main buttons: "Share Result" → native share sheet with formatted text
- [MVP] Sting copy on result card based on ΔE achieved (see copy pool below)
- [MVP] Daily result: show "Next puzzle in Xh Xm" below the buttons
- [Polish] ResultCard slide-up entrance animation (already specced in MVP.md DS.5.7)
- [Polish] Score count-up animation (already specced DS.5.3)

**Sting Copy Pool (by ΔE achieved):**
```
ΔE < 0.5  → "Superhuman. Seriously."
ΔE < 1.0  → "Your eyes are elite."
ΔE < 1.5  → "Sharp. Very sharp."
ΔE < 2.0  → "Better than most."
ΔE < 3.0  → "Solid perception."
ΔE < 4.0  → "Room to grow."
ΔE ≥ 4.0  → "Keep training."
Daily     → use score: >800 "Perfect run.", >500 "Strong.", >200 "Not bad.", else "Try again tomorrow."
```

---

## 6. Already Played / Blocked States

**Problems:**
- Daily "Already played today!" screen: just text + score number, dead end
- Threshold "No tries left" screen: just text, no timer, dead end
- Both states have back button but no CTA — user feels punished

**Fixes:**
- [MVP] Daily already-played: show the ResultCard (read-only, same data) + countdown timer
- [MVP] Threshold blocked: show "Resets in Xh Xm" countdown that ticks live + "Notify me" placeholder (or just Back to Home button styled as primary)
- [Polish] Threshold blocked: show personal best as consolation

---

## 7. Navigation Flow

**Current state (post-fix):**
- Home → Threshold → wrong → Result → back → Home ✅
- Home → Threshold → wrong → Result → "Play Again" → new Threshold game ✅
- Home → Daily → finish → Result → "Back to Home" → Home ✅
- Home → Daily → already played → AlreadyPlayed screen ✅

**Remaining gaps:**
- [MVP] Result "Play Again" tapped when Threshold attempts exhausted → currently starts new game and immediately shows Blocked state. Should: check attempts first, if zero show a "No tries left — come back in Xh" message inline on Result screen instead.
- [MVP] Leaderboard button on Result → navigates to stub LeaderboardScreen. Either hide it or show a "Coming soon" state.
- [Polish] Deep back from Result → Home: system back gesture should go Home, not into a blank (currently works via onBack).

---

## 8. Missing UX Moments

| Moment | Current | Should be |
|--------|---------|-----------|
| New user opens app | Blank home | ΔE info card + game cards |
| Correct tap during game | Green flash → next round | Green flash → ΔE tick-down label → next round |
| Final correct round (Threshold) | Goes to result | Same — fine |
| Wrong tap | Shake + reveal → result | Same — fine, already 450ms delay |
| Result with great score | Static card | Confetti + sting copy |
| Daily complete, return home | "Done" badge | "Done" badge + score + countdown |
| Threshold blocked, return home | "No tries" | "No tries" + reset countdown |
| Share score | No button | Share button → native sheet |

---

## Priority Order

1. Result screen: confetti + share + sting copy
2. Home daily card: countdown timer after completion
3. Threshold blocked state: reset countdown
4. Daily already-played screen: show result card + countdown
5. ΔE first-launch info card (one-time)
6. Result "Play Again" attempt gate check
7. In-game correct-tap ΔE tick-down label
8. Daily last-round copy change
