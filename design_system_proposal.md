# Huezoo Design System Proposal

**Status:** Draft v2 — decisions locked, pending final sign-off before build
**Source:** Stitch/Google design exploration + full codebase audit

---

## Locked Decisions

| Topic | Decision |
|---|---|
| Background color | `#0d0d16` — warmer black, neon reads as emitted light |
| Skewed stat chips | Yes — kinetic energy, adjust letter-spacing for readability if needed |
| Navigation | No bottom bar. No hamburger. Back arrow only. Details below. |
| Result screen | Full-screen push. Items animate in sequence. |
| Gradient CTA | No — keep flat button with shelf shadow. Simpler, already looks good. |
| Glassmorphic top bar | Yes — frosted blur bar. Minor platform variance acceptable. |

---

## 1. Colors

### Background + Surface Stack (expand to 5 tiers)

| Token | Value | Role |
|---|---|---|
| `Background` | `#0d0d16` | App canvas — the void everything emerges from |
| `SurfaceL0` | `#000000` | Deepest wells — track backgrounds, recessed input slots |
| `SurfaceL1` | `#13121c` | Low surface — section backgrounds, subtle separators |
| `SurfaceL2` | `#191923` | Mid surface — card bodies, sheet interior |
| `SurfaceL3` | `#1f1f2a` | High surface — active cards, interactive slots |
| `SurfaceL4` | `#252531` | Highest surface — focused / selected state, popovers |

### Accent Colors (keep all current values — no changes)

Semantic intent (from Stitch — aligns with current usage):
- **Cyan** → action energy, confirmation, progression, primary CTA
- **Magenta** → intensity, premium triggers, alerts, wrong state
- **Yellow** → critical focus, currency, "new" indicators, score
- **Purple** → rare/passive, progress fill gradient
- **Green** → correct state, unlock CTA, gem currency

**No-Line Rule (fully adopt):** Never use 1px grey/neutral borders to section content. Boundaries come from tonal surface shifts. If an edge must be visible, use a ghost rim at 20% opacity of the nearest accent — never a flat grey line.

---

## 2. Typography

Three fonts. No changes to font choices.

| Role | Font | Used for |
|---|---|---|
| Display / Numbers | **Bebas Neue** | ΔE scores, hero numbers, big stat readouts |
| Section headings / Card titles | **Clash Display** | Screen headings, game card titles, dialog titles |
| Body / Labels / Buttons | **Space Grotesk** | All UI text, button labels, badges, captions |

> ⚠️ **Stale comment in `HuezooText.kt`** (line 26): says "Antonio Bold" and "Fredoka" — these are leftover from an earlier design pass. The actual fonts are Bebas Neue and Clash Display. Fix before shipping.

---

## 3. Navigation — No Bottom Bar, No Hamburger

All navigation is driven by back arrows and tapping forward into content.

```
Home (root, scrollable hub)
  └─ Game Screen (full-screen push, back arrow top-left)
       └─ Result Screen (full-screen push, back → Home)
  └─ Settings Screen (full-screen push, back arrow)
  └─ Leaderboard Screen (full-screen push, back arrow)
```

**Home hub layout:**
- Fixed glassmorphic top bar: `HUE ZOO` (left) + `CurrencyPill` (right)
- Scrollable body: profile chip → featured game card (full-width) → 2-column game grid
- Persistent sticky footer row at bottom of scroll content (not a nav bar): `PriceButton` "Unlock Forever — $2" + a ghost stats link
- Settings and Leaderboard are reachable from within their respective content areas (e.g. a settings icon on the profile chip, a leaderboard icon on the result screen)

**Game screen:**
- `HuezooIconButton` variant `Back` (top-left) → returns to Home
- No other persistent navigation chrome

**Result screen:**
- Items animate in sequence (banner → hero score → ΔE readout → stats → CTAs)
- Two buttons: `HuezooButton.Primary` "Play Again" + `HuezooButton.Ghost` "Back to Hub"

---

## 4. New Modifiers to Add

### `neonStrike(color)` — Hard ring glow, zero blur
Draws two concentric strokes *outside* the composable bounds:
- Inner: 2dp solid at full accent opacity
- Outer: 4dp at 30% opacity

Use when: outlier swatch revealed, active game card state, selected chip.
Distinct from `colorGlow` (which is soft and radial). This is a sharp ring.

### `rimLight()` — Inner top-left edge highlight
Draws a 1dp inset stroke on the top and left edges at ~10% white.
Gives cards and panels a "chamfered metal" feel — extremely subtle, premium texture.
Use when: game cards, result card, stat panels.

### `kineticGrid()` — Background texture
Draws a repeating 40×40dp grid of cyan lines at 5% opacity using `drawBehind`.
Apply only on **gameplay screens**. Not on Home, not on Result.
Keeps the game screen feeling like a "game arena" without distracting from the swatches.

---

## 5. Component Inventory — Every Component, Its Purpose, Where It Lives

### `HuezooButton`
**What:** Pill-shaped action button with bottom shelf + spring press animation.
**Variants and their exact use:**

| Variant | Color | Use |
|---|---|---|
| `Primary` | Cyan | Main game CTA — "Play Now", "Play Again", "Deploy" |
| `Confirm` | Green | Submit answer during gameplay |
| `Danger` | Magenta | Destructive or high-stakes — "Exit Game", "Reset" |
| `Score` | Yellow | Achievement / points-related actions |
| `Try` | Blue | Secondary / fallback — "Try Different Color" |
| `Ghost` | Transparent + cyan border | Low-hierarchy secondary action — "Watch Ad (+1 try)", "Back to Hub" |

**Never use:** Material3 `Button`. Never for purchase — use `PriceButton` instead.

---

### `PriceButton`
**What:** Full-width pill, bright green, 56dp height, prominent price text. Dedicated purchase component.
**Use:** Paywall sheet ("Unlock Forever — $2"), persistent home footer CTA. Nowhere else.
**Why separate:** Keeps monetization touchpoints visually distinct from gameplay actions. You never confuse a purchase tap with a game action.

---

### `HuezooIconButton`
**What:** 48dp squircle icon button with shelf + spring press. Replaces Material3 `IconButton` everywhere.
**Variants and their exact use:**

| Variant | Color | Use |
|---|---|---|
| `Back` | Dark surface | Back arrow — top-left on every pushed screen |
| `Confirm` | Green | Quick in-game confirm (e.g. lock in a guess) |
| `Dismiss` | Red | Close / cancel / exit game |
| `Info` | Cyan | Help overlay, hint trigger |

**Never use:** `Icons.AutoMirrored.Default.ArrowBack` with Material `IconButton` on any game screen.

---

### `GameCard`
**What:** Home screen game tile. Outer frame in identity color, inner dark panel, illustration area, shelf press.
**Use:** Exactly one card per game mode on the Home screen. Featured game = full-width. Secondary games = 2-column grid.
**Key params:**
- `identityColor` — each game has its own: `GameThreshold` (indigo-violet), `GameDaily` (coral-orange), `GameMemory` (teal)
- `badgeText` — shows tries remaining, "Done", "New" etc. top-right of illustration
- `triesText` — tries remaining shown in identity color below subtitle
- `personalBest` — dimmed personal record below tries

---

### `SwatchBlock`
**What:** The core gameplay tile — a squircle color swatch the user taps to identify the outlier.
**Use:** Only inside gameplay screens, arranged in a grid. Never on Home or Result screens.
**States:**
- `Default` → tappable, appear-spring on load
- `Pressed` → scale 0.94 instantly
- `Correct` → scale bounce + green border flash (correct tap)
- `Wrong` → lateral shake + magenta border (wrong tap)
- `Revealed` → used after round ends to show which was the outlier

**Sizes:** `Small` (80dp) for dense grids, `Medium` (120dp) default, `Large` (160dp) for easy rounds.

**To add:** `neonStrike` at low opacity on the revealed outlier swatch as a "found it" moment.

---

### `DeltaEBadge`
**What:** Inline badge showing the current ΔE value with color-coded difficulty.
**Use:** Gameplay screen HUD — shows current round difficulty live. Also on Result screen as a small inline label.
**Color encoding (keep as-is):**
- ΔE > 3 → Cyan (easy)
- 1–3 → Yellow (medium)
- ΔE < 1 → Magenta (hard)
**Animations:** Spring bounce + fade appear, count-up to value, color crossfade as difficulty changes.

> ⚠️ **Known gap** (noted in source): typography and background need polish. The badge background is 15% tinted. Consider adding `neonStrike` at very low opacity on the outer edge for hard (ΔE < 1) rounds — makes it feel alarming.

---

### `RoundIndicator`
**What:** Row of dots showing progress through game rounds.
**Use:** Gameplay screen only — sits in the HUD above or below the swatch grid.
**Behavior:**
- Inactive dot: `SurfaceL3`
- Active dot: identity color, 1.2× scale, white ring border
- Completed dot: identity color at 70% opacity
**Pass the game's `identityColor`** as `activeColor` and `completedColor` so it matches the game theme.

---

### `ResultCard`
**What:** Share-ready summary card shown on the Result screen. Outer frame in identity color, inner dark panel with ΔE hero number, score, rounds survived, percentile.
**Use:** Result screen only. One per game session.
**Animations (baked in):** Slide up 60dp + scale 0.9→1.0 spring entrance, count-up on score and ΔE.
**Note:** This is a *card component* placed on the result screen, not the result screen itself. The screen arranges this card + action buttons + ambient glows.

---

### `CurrencyPill`
**What:** Read-only display pill showing gem icon + amount. No press state.
**Use:** Top-right of every top bar (Home, Game, Result screens). Always visible — monetization presence without being a CTA.
**Never:** Make this tappable or interactive. It's a counter, not a button.

---

### `HuezooBottomSheet`
**What:** Modal bottom sheet wrapper styled with `SurfaceL2` bg, 32dp top corners, custom drag handle.
**Use:**
- **Paywall** — slides up after 5 tries are used. Contains `PriceButton` + `Ghost` watch-ad button.
- Any other modal content that doesn't warrant a full-screen push (e.g. a hint explanation, a how-to-play overlay).

**Not used for:** Results (those are full-screen push). Not used for navigation.

---

### `SwatchGradientOverlay`
**What:** A subtle gradient overlay drawn on top of every `SwatchBlock` to add depth/gloss.
**Use:** Internal to `SwatchBlock` — never used directly.

---

### Typography composables (`HuezooText.kt`)
**What:** Typed wrappers around `Text()`. Every text in the app must use one of these — never bare `Text()`.

| Composable | Font | Size | Use |
|---|---|---|---|
| `HuezooDisplayLarge` | Bebas Neue | 56sp | ΔE hero number on Result screen |
| `HuezooDisplayMedium` | Bebas Neue | 40sp | Score / Rounds stats on Result |
| `HuezooDisplaySmall` | Bebas Neue | 28sp | ΔE in badge, compact numeric readouts |
| `HuezooHeadlineLarge` | Bebas Neue | 40sp | App name "HUE ZOO" in top bar |
| `HuezooHeadlineMedium` | Clash Display | 28sp | Section headings, dialog titles, paywall heading |
| `HuezooHeadlineSmall` | Bebas Neue | 20sp | Currency pill amount |
| `HuezooTitleLarge` | Clash Display | 22sp | Sheet titles |
| `HuezooTitleMedium` | Clash Display | 20sp | Game card title |
| `HuezooTitleSmall` | Clash Display | 16sp | Secondary titles, list item headers |
| `HuezooBodyLarge` | Space Grotesk | 18sp | Long-form reading (How to play, etc.) |
| `HuezooBodyMedium` | Space Grotesk | 16sp | Card subtitle, description text |
| `HuezooBodySmall` | Space Grotesk | 14sp | Captions, helper text |
| `HuezooLabelLarge` | Space Grotesk Bold | 16sp | Button labels |
| `HuezooLabelMedium` | Space Grotesk | 13sp | Badge text, secondary labels |
| `HuezooLabelSmall` | Space Grotesk | 12sp | Tries text, personal best, stat headers, badge pills |

---

## 6. Top App Bar — Glassmorphic

All screens share a fixed top bar. No dedicated composable exists yet — needs to be built.

```
┌──────────────────────────────────────────┐
│ [← back]   HUE ZOO          [💎 1,250]  │
│ bg: Background/80 + backdrop-blur        │
│ bottom: 4dp border in SurfaceL1          │
│ bottom-shadow: 4dp solid black           │
└──────────────────────────────────────────┘
```

**Design:**
- Background: `Background` at 80% opacity with `Modifier.blur` behind it
- Bottom edge: 4dp solid `SurfaceL1` — not a hairline, a structural divider
- App name: `HuezooHeadlineLarge` in `AccentCyan` with a `colorGlow` at low intensity
- Back: `HuezooIconButton.Back` (only shown on non-root screens)
- Currency: `CurrencyPill` always — even on Game and Result screens
- iOS/Android note: blur behavior will differ slightly — acceptable per decision

**Component to build:** `HuezooTopBar` composable, used on all screens.

---

## 7. Ambient Background Glows

Stitch uses large radial glows in screen corners to make the background feel alive. Adopt this pattern — important for "game-grade" feel.

**Implementation:** Two fixed `Box` composables behind all content, pointer-events blocked:
- Top-left corner: 256dp circle, primary accent at 8% opacity, `Modifier.blur(100.dp)`
- Bottom-right corner: 256dp circle, secondary accent at 8% opacity, `Modifier.blur(100.dp)`

Each screen can tune the accent colors to match its identity color. On the gameplay screen these could pulse subtly on correct/wrong answers.

**Component to build:** `AmbientGlow` composable that takes two colors and positions them.

---

## 8. Build Order (DS phases, revised)

```
DS.3  Color tokens — add SurfaceL0/L4, update Background to #0d0d16
DS.2  neonStrike + rimLight modifiers
DS.4  HuezooTopBar (glassmorphic)
DS.5  AmbientGlow background composable
DS.6  KineticGrid modifier (gameplay screens only)
DS.7  Skewed stat chip (SkewedStatChip component — time, score readouts)
DS.8  Haptics
DS.9  SwatchBlock + DeltaEBadge neonStrike polish
DS.10 Animations baked into Result screen (sequential reveal)
```

---

## 9. Components Still Needed (not yet built)

| Component | Priority | Description |
|---|---|---|
| `HuezooTopBar` | High | Glassmorphic fixed top bar used on all screens |
| `AmbientGlow` | High | Fixed radial background glows per screen |
| `KineticGridBackground` | Medium | Subtle cyan grid for gameplay screens |
| `SkewedStatChip` | Medium | Slanted container for time/score readouts (gameplay HUD) |
| `HuezooProgressBar` | Medium | Thick gradient progress bar with zero-blur glow fill |

---

*Stitch reference HTML at `/Users/ksharma/Downloads/stitch/stitch/`. Screenshots: each `screen.png`.*
*Fix stale docstring in `HuezooText.kt` line 26 before shipping.*
