# Huezoo — MVP Build Plan
*What we're building now. Lean, addictive, monetized.*

---

## Core Idea

One obsessive game loop. Ship it. Hook people. Monetize the obsession.

**The Threshold** is the hero game: detect the smallest color difference you can.
**Daily Challenge** brings people back every day.
Everything else is future scope.

---

## User Journey

```
Open app
  ├── Daily Challenge card  →  1 free attempt/day  →  Result  →  Share
  └── The Threshold card    →  5 free attempts/day
                                  ├── Out of tries → Watch Ad (+1 try) OR Pay $1.99 (unlimited forever)
                                  └── Result → Leaderboard submit → Share
```

---

## Monetization

| Feature | Free | Paid ($1.99 one-time) |
|---|---|---|
| Daily Challenge | ✅ 1/day | ✅ 1/day |
| The Threshold attempts | 5/day | ♾️ Unlimited |
| Watch ad for +1 try | ✅ | Not needed |
| Submit to leaderboard | ✅ | ✅ |
| Ads | ✅ shown | ❌ removed |

**Why this works:**
- Free users hit the 5-try wall exactly when they're most addicted
- $1.99 is impulse-buy price — no hesitation
- Ad option earns revenue from users who won't pay
- Leaderboard is free to read → motivation to grind and submit

---

## Tech Stack

| Layer | Choice |
|---|---|
| UI | Compose Multiplatform (Android + iOS) |
| Navigation | Compose Navigation (KMP) |
| Local DB | SQLDelight |
| Backend | Firebase Realtime Database (minimal — leaderboard only) |
| Auth | Firebase Anonymous Auth |
| Ads | AdMob (Android + iOS) |
| IAP | Google Play Billing + StoreKit 2 (iOS) |
| Color Math | Pure Kotlin (commonMain) |

---

## App Structure

```
Home Screen
  ├── Daily Challenge card  (date, done/not done, yesterday score)
  └── The Threshold card    (your best ΔE, your rank)

Game: The Threshold
  └── 3 swatches → tap odd one → ΔE tightens → miss = game over

Game: Daily Challenge
  └── Date-seeded puzzle, same for all users, 1 attempt, share card

Result Screen
  ├── Animated score card
  ├── [Play Again]  — checks attempt gate
  ├── [Share]       — native share sheet
  └── [Leaderboard] — view top 50

Leaderboard Screen
  └── Top 50, rank by lowest ΔE detected, your entry highlighted

Paywall / Ad Gate
  ├── [Watch Ad → +1 try]
  └── [Unlock Unlimited — $1.99]
```

---

## Task List

### Phase 0 — Project Setup
- [ ] 0.1 Review existing `App.kt`, `AppModule.kt`, `build.gradle.kts`
- [ ] 0.2 Set up Compose Navigation (KMP-compatible)
- [ ] 0.3 Define package structure:
  ```
  ui/home
  ui/games/threshold
  ui/games/daily
  ui/result
  ui/leaderboard
  ui/paywall
  domain/color
  data/db
  data/firebase
  ```
- [ ] 0.4 SQLDelight schema:
  ```sql
  CREATE TABLE daily_challenge (
    date TEXT NOT NULL PRIMARY KEY,
    score REAL NOT NULL,
    completed INTEGER NOT NULL DEFAULT 0
  );
  CREATE TABLE threshold_session (
    date TEXT NOT NULL PRIMARY KEY,
    attempts_used INTEGER NOT NULL DEFAULT 0
  );
  CREATE TABLE personal_best (
    game_id TEXT NOT NULL PRIMARY KEY,
    best_delta_e REAL,
    best_score INTEGER,
    rank INTEGER
  );
  CREATE TABLE user_settings (
    key TEXT NOT NULL PRIMARY KEY,
    value TEXT NOT NULL
  );
  ```

### Phase 1 — Color Math (pure Kotlin, commonMain)
- [ ] 1.1 `rgbToLab(r, g, b): Triple<Float, Float, Float>` — sRGB → CIELAB
- [ ] 1.2 `deltaE(lab1, lab2): Float` — CIEDE2000
- [ ] 1.3 `randomVividColor(): Color` — avoid near-black/white/grey
- [ ] 1.4 `generateOddSwatch(baseDeltaE: Float): Pair<Color, Color>` — base + odd at exact ΔE
- [ ] 1.5 `scoreFromDeltaE(de: Float): Int` — shared scoring formula
- [ ] 1.6 `seededColorForDate(date: LocalDate): Color` — deterministic daily color

### Phase 2 — Core UI Components (Compose, commonMain)
- [ ] 2.1 `SwatchBlock` — colored tile, sizes sm/md/lg, tap ripple, shake animation on wrong
- [ ] 2.2 `ScoreCard` — rounded result card, count-up animation, confetti
- [ ] 2.3 `GameCard` — home card: title, description, personal best ΔE or score
- [ ] 2.4 `DeltaEBadge` — live ΔE label, fades in after each round
- [ ] 2.5 `RoundIndicator` — dot row showing current round / total
- [ ] 2.6 `PrimaryButton`, `GhostButton` — reusable button variants
- [ ] 2.7 `BottomSheet` — reusable sheet for paywall / name entry

### Phase 3 — Home Screen
- [ ] 3.1 Two cards: Daily Challenge + The Threshold
- [ ] 3.2 Daily card: shows today's date, "Done ✓" if played, yesterday's score if available
- [ ] 3.3 Threshold card: shows personal best ΔE and rank (from SQLDelight)
- [ ] 3.4 Stagger entrance animation on cards (0.08s delay each)
- [ ] 3.5 Tap → navigate to respective game

### Phase 4 — Game: The Threshold
- [ ] 4.1 Start at ΔE 5.0, shrink by 0.3 each correct pick
- [ ] 4.2 Show 3 `SwatchBlock`s — 2 identical base + 1 odd, shuffled
- [ ] 4.3 Tap handler: correct → green pulse + next round; wrong → shake + game over
- [ ] 4.4 Show live ΔE badge after each pick
- [ ] 4.5 On game over: record final ΔE, navigate to Result
- [ ] 4.6 Deduct 1 attempt on game start; check gate before starting
- [ ] 4.7 Attempt gate: if attempts_used >= 5 AND not paid → show paywall sheet

### Phase 5 — Game: Daily Challenge
- [ ] 5.1 Date-seeded puzzle — use `seededColorForDate(today)` as base
- [ ] 5.2 6 rounds, fixed difficulty curve (same for all players same day)
- [ ] 5.3 Block replay if already completed today (check SQLDelight)
- [ ] 5.4 Save score + mark completed in SQLDelight
- [ ] 5.5 Countdown timer to tomorrow's challenge on home card

### Phase 6 — Result Screen
- [ ] 6.1 Animated score card slides up
- [ ] 6.2 Count-up animation for final ΔE and score
- [ ] 6.3 Confetti burst
- [ ] 6.4 Percentile label: "Better than X% of players" (from Firebase top list)
- [ ] 6.5 [Play Again] button → re-checks attempt gate
- [ ] 6.6 [Share] button → native share sheet: "I detected ΔE 1.2 — top 6% | Huezoo"
- [ ] 6.7 [View Leaderboard] button → navigate to leaderboard screen

### Phase 7 — Monetization Gates
- [ ] 7.1 Track `attempts_used` per day in SQLDelight, reset at midnight
- [ ] 7.2 Paywall bottom sheet: [Watch Ad → +1 try] + [Unlock Unlimited — $1.99]
- [ ] 7.3 AdMob: rewarded ad for +1 try (Android + iOS)
- [ ] 7.4 In-app purchase: one-time "Unlimited" product
- [ ] 7.5 Persist paid status in SQLDelight `user_settings`
- [ ] 7.6 If paid: hide all ads, remove attempt cap

### Phase 8 — Leaderboard + Firebase
- [ ] 8.1 Firebase project setup, enable Realtime DB + Anonymous Auth
- [ ] 8.2 Firebase config added to Android/iOS targets
- [ ] 8.3 Schema: `/leaderboard/{pushId}: { name, deltaE, timestamp, uid }`
- [ ] 8.4 Query: order by `deltaE` ascending, limit to top 50
- [ ] 8.5 Leaderboard screen: ranked list, your entry highlighted
- [ ] 8.6 Submit flow: name input sheet (once) → push to Firebase → store name locally
- [ ] 8.7 Security rules: public read, anon write only, max 1 write per UID per day

### Phase 9 — Polish & Ship
- [ ] 9.1 App icon + splash screen
- [ ] 9.2 Dark mode (system-aware)
- [ ] 9.3 Haptic feedback on correct/wrong (platform-specific expect/actual)
- [ ] 9.4 Test on real Android device + iOS simulator
- [ ] 9.5 Play Store listing (screenshots, description, rating)
- [ ] 9.6 App Store listing

---

## Animations Spec

| Trigger | Animation |
|---|---|
| Home cards load | Stagger fade-up (0.08s delay per card) |
| Swatch appears | Scale 0.8 → 1.0, 200ms spring |
| Wrong answer | Shake X ±8px, 3 cycles, 300ms |
| Correct answer | Scale pulse + green border flash |
| ΔE tightens | Badge fades in, color shifts green → amber → red |
| Score count-up | Spring easing, 800ms |
| Result card | Slide up + scale from 0.9, spring |
| Confetti | 40 particles in tested hue, gravity + spin |

---

## Scoring: The Threshold

```
Starting ΔE: 5.0
Step down:   −0.3 per correct pick
Game over:   first miss

Final score = rounds survived
Your threshold = last ΔE you attempted (the one you missed on)
Leaderboard sorts by: lowest threshold ΔE (ascending)
```

---

## Share Card Format

```
╔══════════════════════════════╗
║  🎯  Huezoo               ║
║                              ║
║  The Threshold               ║
║  I detected ΔE 1.2           ║
║  Better than 94% of players  ║
║                              ║
║  [huezoo.app]             ║
╚══════════════════════════════╝
```

---

## Build Order Summary

```
Phase 0   → Setup + DB schema
Phase DS  → Design system (colors, fonts, haptics, sound, components)
            See DESIGN_SYSTEM.md for full spec
Phase 1   → Color math utils
Phase 2   → Core UI components (built on design system)
Phase 3   → Home screen
Phase 4   → The Threshold game
Phase 5   → Daily Challenge game
Phase 6   → Result screen
Phase 7   → Ads + IAP gates
Phase 8   → Firebase leaderboard
Phase 9   → Polish + ship
```
