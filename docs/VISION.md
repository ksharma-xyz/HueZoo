# Huezoo — Full Vision
*Where this app goes after MVP ships and has users.*

---

## Overview

Huezoo is a color perception game app. MVP ships The Threshold + Daily Challenge.
Once those are live and people are hooked, we expand with 4 additional mini-games,
richer leaderboards, and social features.

---

## Full Game Lineup

### Shipped in MVP
1. **The Threshold** — detect the smallest ΔE you can. One miss = game over.
2. **Daily Challenge** — same puzzle for everyone, once per day.

### Add after MVP
3. **What's My Delta E?** — 3 swatches, pick the odd one. 6 rounds, score by accuracy.
4. **Odd Swatch Out** — N×N grid, one subtly different tile, timer counts down.
5. **Mix to Match** — drag RGB/HSL sliders to match a target color.
6. **Color Memory Match** — color flashes 3s, disappears. New color: same or different?

---

## Full App Structure (post-MVP)

```
/                    → Home screen (all game cards)
/game/threshold      → The Threshold
/game/daily          → Daily Challenge
/game/delta-e        → What's My Delta E?
/game/odd-swatch     → Odd Swatch Out
/game/mix-to-match   → Mix to Match
/game/color-memory   → Color Memory Match
/leaderboard         → Global leaderboard (The Threshold only, or per game)
/result              → End screen (score card + share + replay gate)
/settings            → Manage subscription, restore purchase
```

---

## Game 3 — What's My Delta E?

- 3 swatches shown: 2 identical base + 1 odd
- Pick the odd one within time limit
- Score = `100 ÷ deltaE × round multiplier`
- 6 rounds, deltaE tightens each round (~15 → ~2)
- Live ΔE badge shown after each pick
- Max score ~600

## Game 4 — Odd Swatch Out

- 3×3 grid (easy) → 4×4 grid (hard) of near-identical swatches
- One tile is subtly different — find it before timer hits 0
- Miss = round lost; find it = +points + time bonus
- 8 rounds total, grid grows + deltaE shrinks
- Max score ~800

## Game 5 — Mix to Match

- Target color shown as large swatch
- 3 sliders: R, G, B (toggle to HSL mode)
- Live preview of your mix beside target
- Submit when happy → final ΔE shown
- Score = `max(0, 100 − deltaE × 5)` per round
- 5 rounds per game, max score 500

## Game 6 — Color Memory Match

- Color A shown for 3 seconds, then hidden
- Color B shown — SAME or DIFFERENT?
- Correct = +10 pts, wrong = −5 pts
- 10 rounds, threshold tightens each round
- Max score 100

---

## Full Home Screen (post-MVP)

```
┌─────────────────────────────┐
│  📅 Daily Challenge  Day 47 │
│  ✅ Done — Score: 340       │
└─────────────────────────────┘
┌─────────────────────────────┐
│  🎯 The Threshold           │
│  Your best: ΔE 1.4  #23    │
└─────────────────────────────┘
┌─────────────┐  ┌────────────┐
│  Delta E    │  │ Odd Swatch │
│  Best: 340  │  │ Best: 620  │
└─────────────┘  └────────────┘
┌─────────────┐  ┌────────────┐
│ Mix to Match│  │Color Memory│
│  Best: 480  │  │  Best: 90  │
└─────────────┘  └────────────┘
```

---

## Full Monetization (post-MVP)

| Feature | Free | Paid ($1.99) |
|---|---|---|
| Daily Challenge | ✅ 1/day | ✅ 1/day |
| The Threshold | 5 tries/day | ♾️ Unlimited |
| Watch ad for +1 try | ✅ | Not needed |
| Mini-games (4 games) | ✅ 3 plays/day each | ♾️ Unlimited |
| Submit to leaderboard | ✅ | ✅ |
| Ads shown | ✅ | ❌ |
| Future: color blind modes | ❌ | ✅ |

---

## Leaderboard Vision (post-MVP)

- **The Threshold**: global, sort by lowest ΔE (single metric, most meaningful)
- **Per mini-game**: top 100 by score, refreshes live via Firebase
- **Weekly reset** option for mini-game boards → fresh competition every week
- Seasons: "Season 1 champ" badge preserved on profile

---

## Firebase Data Model (full)

```
/leaderboard
  /threshold
    /{pushId}: { name, deltaE, timestamp, uid }

  /delta_e
    /{pushId}: { name, score, bestDeltaE, timestamp, uid }

  /odd_swatch
    /{pushId}: { name, score, timestamp, uid }

  /mix_to_match
    /{pushId}: { name, score, avgDeltaE, timestamp, uid }

  /color_memory
    /{pushId}: { name, score, accuracy, timestamp, uid }
```

Firebase rules: public read, anonymous-auth write only, max 1 write per UID per day.

---

## Post-MVP Feature Ideas

- **Color Blind Mode** — games adapted for deuteranopia / protanopia / tritanopia
- **Practice Mode** — unlimited plays, no score, just calibrate your eye
- **Streak system** — play any game daily, build streak, lose it if you miss a day
- **Achievements** — "Detected ΔE under 1.0", "7-day streak", "Top 10 global"
- **Seasonal themes** — swap color palettes (pastels, neons, earth tones)
- **Friends leaderboard** — share code, see only people you know

### Visual / UI ideas (from design exploration, April 2026)

- **Timer ring** — the segmented circular ring (cine_solid aesthetic) wraps the
  reference swatch and counts down as a timed-mode pressure mechanic. Segments
  drain arc-by-arc. Pairs with a future "Speed Round" or timed variant of Threshold.
  Ring shape reference: `docs/ideas/cine_solid.png`

- **Progress ring (non-timed)** — same ring shape but fills up as you answer
  correctly in a session. Segments map to milestone thresholds. Resets on wrong
  answer for max drama. Could ship sooner than the timed variant.

- **Animal mascots per difficulty tier** — a faint animal watermark inside each
  swatch that changes as ΔE drops, tying visual identity to difficulty:
  Bear (ΔE 5→3) → Fox (3→2) → Frog (2→1) → Octopus (1→0.5) → Eagle (<0.5).
  Animal flashes on correct-answer reveal. Pairs with PlayerLevel progression.

- **"Beyond human perception" wall** — when ΔE drops below ~0.5 the game
  surfaces a special message acknowledging the player has crossed the threshold
  of human color discrimination. Eagle mascot appears. Legendary tier moment.

- **BulbPetal swatch shape** — wider, more bulbous teardrop than the existing
  Flower style; inward curve at the base, no border. Based on `docs/ideas/screen.png`
  petal cluster. Add as a new `SwatchLayoutStyle` entry.

---

## Tech Stack (full vision)

| Layer | Choice |
|---|---|
| UI | Compose Multiplatform (Android + iOS) |
| Navigation | Compose Navigation (KMP) |
| Local DB | SQLDelight |
| Backend | Firebase Realtime Database |
| Auth | Firebase Anonymous Auth |
| Ads | AdMob |
| IAP | Google Play Billing + StoreKit 2 |
| Color Math | Pure Kotlin (commonMain) |
| Share card | Compose screenshot → native share |

---

## Build Sequence (after MVP ships)

```
MVP shipped (Threshold + Daily + Monetization + Firebase leaderboard)
  → Add Game 3: What's My Delta E?
  -> Add Game 4: Odd Swatch Out
  → Add Game 5: Mix to Match
  → Add Game 6: Color Memory Match
  → Per-game leaderboards
  → Streak system
  → Achievements
  → Color Blind Mode
```

Add one game at a time. Each new game = new reason to re-download and share.
