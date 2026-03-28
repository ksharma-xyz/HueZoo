# Huezoo — Monetization Design

*One-time purchase ($2.99) removes all friction. Free tier is deliberately generous to maximise organic reach and IAP conversion.*

---

## Free vs Paid Summary

| Feature | Free | Paid ($2.99) |
|---|---|---|
| Daily Challenge | ✅ unlimited | ✅ unlimited |
| Threshold attempts | 5 per 8h window | ♾️ unlimited |
| Watch Ad → +1 try | ✅ | not needed |
| Spend 15 💎 → +1 try | ✅ | not needed |
| Banner ads | shown | removed |
| Interstitial ads | after every 2nd Threshold game | removed |
| Leaderboard | ❌ (locked) | ✅ |
| Ads shown | yes | no |

---

## Ad Placement Logic

### Banner Ads (AdSize.BANNER — 50dp tall)

| Screen | Shown when | Placement |
|---|---|---|
| Home | `!isPaid` | pinned bottom, always visible |
| Result | `!isPaid` | pinned bottom, above nav bar |
| Threshold (Playing / Blocked) | `!isPaid` | pinned bottom via Box overlay |
| Daily (Playing / AlreadyPlayed) | `!isPaid` | pinned bottom via Box overlay |
| Upgrade / Paywall | never | — purchase funnel must be ad-free |

**Rationale:** Banners at the screen bottom are a low-intrusion constant revenue stream. The swatch area is never covered — the banner sits below gameplay. Upgrade and Paywall screens must never show ads (the player is about to pay, friction is the enemy).

---

### Interstitial Ads (full-screen, skippable after 5s)

**Rule: show at most once per 2 Threshold sessions, never on Daily.**

```
Threshold game ends
  → increment interstitialCounter (persisted in memory, resets per session)
  → if counter % 2 == 0 && !isPaid
      → show InterstitialAd before navigating to Result
      → on dismissed / completed → navigate to Result
  else
      → navigate to Result directly
```

**Never show interstitials after:**
- Daily Challenge (low attempt count, high emotional investment — player just waited 24h)
- Any game where the player set a new personal best (reward moment, do not interrupt)
- The very first session of the day (cold-start, fragile retention)

**Frequency cap:** Maximum 3 interstitials per calendar day per device (AdMob frequency capping + local guard).

**Implementation points:**
- Add `InterstitialAdClient` interface alongside `RewardedAdClient`
- `AndroidInterstitialAdClient`: `load()` + `show(): AdResult` using `InterstitialAd.load()`
- `ThresholdViewModel` (or a dedicated `AdOrchestrator`) holds a counter + calls `show()` before emitting the navigation event
- `ThresholdScreen` / nav host waits for ad completion before pushing Result

---

### Rewarded Ads (+1 try)

Triggered explicitly by the player via `PaywallSheet`. No auto-show.

- Load in background when `PaywallViewModel` init (triggered when sheet is opened)
- Show only when `adReady == true` (button enabled); button is disabled otherwise
- On `AdResult.Rewarded` → `settingsRepository.addBonusTries(1)` → dismiss sheet → player immediately retries

---

## Gems System

### Earning Gems

| Event | Gems |
|---|---|
| Each correct tap / round | `max(1, floor(10 / deltaE))` — harder detection → more gems |
| Session participation (any attempt) | +3 |
| Daily Challenge completion | +5 |
| Daily Challenge perfect (6/6 rounds) | +15 bonus |
| Milestone: 10 correct taps in one session | +20 |
| Milestone: 25 correct taps in one session | +50 |
| Personal best broken (Threshold) | +10 |
| Personal best broken (Daily) | +5 |
| First game of the day | +5 |
| Level-up | varies by level (see table below) |

### Spending Gems

| Action | Cost |
|---|---|
| +1 Threshold bonus try | 15 💎 |
| (Future) cosmetic swatch size unlock | TBD |
| (Future) daily challenge hint | TBD |

### Level Progression

Levels gate cosmetic rewards and profile flair. They do **not** gate gameplay.

| Level | Name | Gems required (cumulative) | Level-up bonus |
|---|---|---|---|
| 1 | Novice Eye | 0 | — |
| 2 | Apprentice | 100 | +20 💎 |
| 3 | Analyst | 300 | +40 💎 |
| 4 | Observer | 600 | +60 💎 |
| 5 | Perceptionist | 1,000 | +80 💎 |
| 6 | Sharp Signal | 1,500 | +100 💎 |
| 7 | Chromatic | 2,200 | +120 💎 |
| 8 | Elite | 3,000 | +150 💎 |
| 9 | Apex | 4,500 | +200 💎 |
| 10 | SUPERHUMAN | 7,000 | +300 💎 + special badge |

**Level-up logic (pending — `PlayerLevel.fromGems()` already exists):**
- Detect level-up in `ThresholdViewModel` / `DailyViewModel` after gem accumulation
- Emit a `LevelUpEvent` to the UiState
- Show level-up banner animation on Result screen (brief, skippable)
- Award bonus gems via `settingsRepository.addGems(levelUpBonus)`

---

## Conversion Funnel

The goal is to convert high-engagement free users who have hit the attempt cap.

```
Player runs out of Threshold tries
  → PaywallSheet appears
      ├── Spend gems (immediate, no friction if balance > 15)
      ├── Watch Ad (30s investment for +1 try)
      └── UNLOCK FOREVER ($2.99)
             └── UpgradeScreen (one tap to purchase)

Player finishes game (out of tries + no paid)
  → Result screen shows "GO UNLIMITED" CTA below play button
      └── navigates to UpgradeScreen
```

**Key principle:** Never show the upgrade CTA while the player still has tries left. Only surface it at the moment of friction (out of tries). Premature upsells reduce trust.

---

## Implementation Checklist

### Interstitial Ads

- [x] `InterstitialAdClient` interface in commonMain (alongside `RewardedAdClient`)
- [x] `AndroidInterstitialAdClient` in androidMain — `load()` + `show(activity): AdResult`
- [x] `IosInterstitialAdClient` stub in iosMain
- [x] Bind in `AndroidModule` / `IosModule`
- [x] `AdOrchestrator` singleton — frequency counter + show gate (in-memory, resets on date change)
- [x] Wire into Threshold game flow: show ad → then navigate to Result
- [x] Frequency cap: 3/day guard (in-memory `shownTodayCount` keyed by date)
- [x] Never show after personal best (guarded by `isSessionNewPersonalBest`)

### Level-Up Events

- [x] `PlayerLevel.levelUpBonus(level: PlayerLevel): Int` — return bonus gems per level
- [x] Detect level crossing in `ThresholdViewModel` + `DailyViewModel` after `addGems()`
- [x] Award bonus gems and include in session breakdown
- [ ] Emit `LevelUpEvent(newLevel, bonusGems)` in UiState / side-effect channel *(UI banner — future)*
- [ ] Level-up banner on Result screen (500ms entrance animation, auto-dismiss at 2s) *(future)*

### Daily Streak Bonus

- [x] If `streak >= 3` → +5 💎 daily bonus (in `DailyViewModel.finishGame()`)
- [x] If `streak >= 7` → +10 💎 daily bonus (in `DailyViewModel.finishGame()`)
- [x] `DailyRepository.getStreak()` fully implemented in `DefaultDailyRepository`
- [ ] Streak displayed on Home stats bar *(requires HomeViewModel wiring)*

### Banner Ad IDs (replace before publishing)

| Platform | Current (test) | Replace with |
|---|---|---|
| Android banner | `ca-app-pub-3940256099942544/6300978111` | Real banner unit ID |
| Android rewarded | `ca-app-pub-3940256099942544/5224354917` | Real rewarded unit ID |
| Android interstitial | `ca-app-pub-3940256099942544/1033173712` | Real interstitial unit ID |
| Android App ID | `ca-app-pub-3940256099942544~3347511713` | Real App ID in Manifest |

---

## What NOT to do

- **Never** show an ad mid-game or during a swatch tap.
- **Never** show an ad after a new personal best — the player is emotionally high.
- **Never** show the Upgrade CTA while attempts are available.
- **Never** gate the Daily Challenge behind a paywall — it's the free hook.
- **Never** reduce gem earn rates silently after a purchase (dark pattern).
