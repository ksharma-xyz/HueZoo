# Huezoo Design System

**Status:** v3 — active reference document. Update this file whenever a DS decision changes.
**Phase completed:** DS.3 → DS.4 (colors, modifiers, top bar). DS.5+ pending.

---

## Locked Decisions

| Topic | Decision |
|---|---|
| Background color | `#0d0d16` — warmer black, neon reads as emitted light |
| App display name | **HUEZ** — Bebas Neue italic, tight letter-spacing on "EZ" span |
| Skewed stat chips | Yes — kinetic energy, reuses `ParallelogramShape` + `shapedShadow` |
| Navigation | No bottom bar. No hamburger. Back arrow only. Details below. |
| Result screen | Full-screen push. Items animate in sequence. |
| Gradient CTA | No — flat button + shelf shadow. Simpler, already looks good. |
| Glassmorphic top bar | Yes — frost gradient (no platform blur needed). Minor platform variance OK. |
| Shadow pattern | **Directional hard shadow** via `shapedShadow` — cyan, offset right+bottom only. Use everywhere interactive buttons/chips appear. Offset = 4dp. |
| Border rule | **No-Line Rule** — section boundaries via tonal surface shifts only. Ghost rim at 20% accent opacity if an edge must be visible. Never flat grey 1px lines. |

---

## 1. Colors

### Background + Surface Stack

| Token | Value | Role |
|---|---|---|
| `Background` | `#0d0d16` | App canvas — the void everything emerges from |
| `SurfaceL0` | `#000000` | Deepest wells — track backgrounds, recessed input slots |
| `SurfaceL1` | `#13121c` | Low surface — section backgrounds, subtle separators |
| `SurfaceL2` | `#191923` | Mid surface — card bodies, sheet interior |
| `SurfaceL3` | `#1f1f2a` | High surface — active cards, interactive slots |
| `SurfaceL4` | `#252531` | Highest surface — focused / selected state, popovers |

**Material3 slot mapping** (for `surfaceContainer*` calls in M3 components):

| Token | M3 slot |
|---|---|
| `SurfaceL0` | `surfaceContainerLowest` |
| `SurfaceL1` | `surface` / `surfaceContainerLow` |
| `SurfaceL2` | `surfaceContainer` |
| `SurfaceL3` | `surfaceContainerHigh` |
| `SurfaceL4` | `surfaceContainerHighest` / `surfaceVariant` |

### Accent Colors

Semantic intent:
- **Cyan** → action energy, confirmation, progression, primary CTA
- **Magenta** → intensity, premium triggers, alerts, wrong state
- **Yellow** → critical focus, currency, "new" indicators, score
- **Purple** → rare/passive, progress fill gradient
- **Green** → correct state, unlock CTA, gem currency

### Glow Colors (pre-mixed at 40% alpha)

`GlowCyan`, `GlowMagenta`, `GlowYellow`, `GlowPurple`, `GlowGreen` — use these directly
in `colorGlow()` calls. Do not re-mix alpha at callsites.

---

## 2. Typography

Three fonts. No changes to font choices.

| Role | Font | Used for |
|---|---|---|
| Display / Numbers | **Bebas Neue** | ΔE scores, hero numbers, big stat readouts, wordmark |
| Section headings / Card titles | **Clash Display** | Screen headings, game card titles, dialog titles |
| Body / Labels / Buttons | **Space Grotesk** | All UI text, button labels, badges, captions |

### Scale

| Composable | Font | Size | Use |
|---|---|---|---|
| `HuezooDisplayLarge` | Bebas Neue | 56sp | ΔE hero number on Result screen |
| `HuezooDisplayMedium` | Bebas Neue | 40sp | Score / Rounds stats on Result |
| `HuezooDisplaySmall` | Bebas Neue | 28sp | ΔE in badge, compact numeric readouts |
| `HuezooHeadlineLarge` | Bebas Neue | 40sp | Screen-level hero text |
| `HuezooHeadlineMedium` | Clash Display | 28sp | Section headings, dialog titles, paywall heading |
| `HuezooHeadlineSmall` | Bebas Neue | **26sp** | Currency pill amount (increased from 20sp for top-bar balance) |
| `HuezooTitleLarge` | Clash Display | 22sp | Sheet titles |
| `HuezooTitleMedium` | Clash Display | 20sp | Game card title |
| `HuezooTitleSmall` | Clash Display | 16sp | Secondary titles, list item headers |
| `HuezooBodyLarge` | Space Grotesk | 18sp | Long-form reading (How to play, etc.) |
| `HuezooBodyMedium` | Space Grotesk | 16sp | Card subtitle, description text |
| `HuezooBodySmall` | Space Grotesk | 14sp | Captions, helper text |
| `HuezooLabelLarge` | Space Grotesk Bold | 16sp | Button labels |
| `HuezooLabelMedium` | Space Grotesk | 13sp | Badge text, secondary labels |
| `HuezooLabelSmall` | Space Grotesk | 12sp | Tries text, personal best, stat headers, badge pills |

**Rule:** Every text element in the app must use one of the above composables. Never use bare `Text()`.

---

## 3. Navigation — No Bottom Bar, No Hamburger

All navigation is driven by back arrows and forward content taps.

```
Home (root, scrollable hub)
  └─ Game Screen (full-screen push, back button top-left)
       └─ Result Screen (full-screen push, back → Home)
  └─ Settings Screen (full-screen push, back button)
  └─ Leaderboard Screen (full-screen push, back button)
```

**Home hub layout:**
- Fixed `HuezooTopBar`: **HUEZ** wordmark left + `CurrencyPill` right
- Scrollable body: profile chip → featured game card (full-width) → 2-column game grid
- Persistent sticky footer row at bottom of scroll content (not a nav bar): `PriceButton` + ghost stats link

**Back button rule (HuezooTopBar):**
- Root screen (Home): no back button → wordmark shown
- All pushed screens: back button shown → wordmark hidden (the back button owns the left slot entirely)

---

## 4. Shapes

| Value / Class | Description | Used for |
|---|---|---|
| `SquircleSmall` | exponent 3.5 | Chips, badges |
| `SquircleMedium` | exponent 4 (iOS squircle) | Default interactive elements |
| `SquircleLarge` | exponent 5 | Large surfaces |
| `SquircleCard` | exponent 4 | Game cards |
| `SquircleButton` | exponent 4 | Standard buttons |
| `PillShape` | `RoundedCornerShape(50)` | Text buttons, price CTAs, currency pills |
| `ParallelogramShape(skewFraction)` | Italic-slanted rectangle | Back button, DS.7 SkewedStatChip |
| `ParallelogramBack` | `ParallelogramShape(0.25f)` | Top bar back button preset |

**`ParallelogramShape`** — skewFraction 0.25 = 25% of height → ~14° lean matching Bebas Neue italic.
Always pair with `shapedShadow` and the same shape in `clip` + `background`.

---

## 5. Modifiers (`ui/theme/Modifiers.kt`)

### `shapedShadow(shape, color, offsetX, offsetY)` ✅ Built
**The primary shadow pattern for all interactive buttons and chips.**

Draws one hard copy of `shape` offset (+offsetX, +offsetY) behind the composable.
Shadow is only visible on the right and bottom edges (neo-brutalist press-depth).
Works with any `Shape` — parallelogram, squircle, pill, rounded corner.

**Consistent usage rules:**
- `offsetX` / `offsetY` = **4 dp** always
- `color` = `AccentCyan.copy(alpha = 0.30f)` for cyan-themed components; use the component's identity accent for other colors
- Always pair: `.shapedShadow(shape, ...).clip(shape).background(fill)`
- `shapedShadow` must come BEFORE `clip` in the chain — it uses `drawBehind` and is not clipped

```kotlin
// Example — any shaped interactive element:
Modifier
    .shapedShadow(ParallelogramBack, HuezooColors.AccentCyan.copy(0.30f))
    .clip(ParallelogramBack)
    .background(HuezooColors.SurfaceL3)
```

### `neonStrike(color, cornerRadius)` ✅ Built
Hard ring glow — two concentric strokes outside composable bounds (2dp inner, 4dp outer at 30%).
Use when: outlier swatch revealed, active game card, selected chip, DeltaEBadge at hard (ΔE < 1).
Distinct from `colorGlow` (soft radial). This is a sharp ring.

### `rimLight(cornerRadius)` ✅ Built
1dp inset stroke on top + left edges at 10% white — chamfered metal feel.
Use on: game cards, result card, stat panels. Apply after `background`.

### `colorGlow(color, glowRadius, cornerRadius)` ✅ Built
Soft layered radial glow via 8 concentric semi-transparent round rects.
Use for: ambient glow around key elements. NOT for directional shadows (use `shapedShadow`).
Note: draws round rects only — does not follow non-rectangular shapes.

### `depthShadow(color, offsetY, cornerRadius)` ✅ Built
Soft multi-layer offset shadow (4 layers). Use for ambient card depth on squircle/rounded shapes.
Do NOT use for interactive buttons — use `shapedShadow` instead.

### `kineticGrid()` — DS.6 (not yet built)
Repeating 40×40dp grid of cyan lines at 5% opacity via `drawBehind`.
Gameplay screens only. Not on Home. Not on Result.

---

## 6. Component Inventory

### `HuezooTopBar` ✅ Built
**What:** Glassmorphic fixed top bar on every screen.
**Layout:**
```
┌────────────────────────────────────────┐
│  [status bar inset]                    │
│  [◁ back? | HUEZ]       [💎 1,250]   │  ← 64dp content row
├────────────────────────────────────────┤  ← 4dp SurfaceL1 border
```
**Back button:** `ParallelogramBack` shape, `SurfaceL3` fill, `shapedShadow(AccentCyan, 0.30f)`,
thick `<` chevron drawn via Canvas (5dp stroke, 24dp size, `StrokeCap.Round`).
Spring scale on press (0.88). Only shown on non-root screens.
**Wordmark:** "HUEZ" in Bebas Neue italic, `AccentCyan`, tight letter spacing on "EZ" span.
**Background:** Vertical frost gradient `Background` 95% → 82% — no platform blur needed.
**API:** `HuezooTopBar(onBackClick, currencyAmount, gemIcon)`. No `backIcon` param — chevron is baked in.
**Rule:** NEVER use Material3 `TopAppBar`.

---

### `CurrencyPill` ✅ Built
**What:** Read-only pill showing gem icon + amount. No press state.
**Spec:** `SurfaceL2` background, `PillShape`, cyan gem icon (20dp), `HuezooHeadlineSmall` white text (26sp).
**Use:** Top-right of every screen's `HuezooTopBar`. Always visible.
**Rule:** Never make this tappable. It's a counter, not a button.

---

### `HuezooButton`
**What:** Pill-shaped action button with bottom shelf + spring press animation.

| Variant | Color | Use |
|---|---|---|
| `Primary` | Cyan | Main game CTA — "Play Now", "Play Again" |
| `Confirm` | Green | Submit answer during gameplay |
| `Danger` | Magenta | Destructive — "Exit Game", "Reset" |
| `Score` | Yellow | Achievement / points-related actions |
| `Try` | Blue | Secondary / fallback — "Try Different Color" |
| `Ghost` | Transparent + cyan border | Low-hierarchy — "Watch Ad", "Back to Hub" |

**Shadow:** `shapedShadow(PillShape, identityColor.darken(), offsetX=4dp, offsetY=4dp)`.
**Rule:** Never use Material3 `Button`. Never for purchase — use `PriceButton`.

---

### `PriceButton`
**What:** Full-width pill, bright green, 56dp height, prominent price text.
**Use:** Paywall sheet + home footer CTA only. Nowhere else.
**Shadow:** `shapedShadow(PillShape, ShelfPrice, offsetX=4dp, offsetY=4dp)`.

---

### `HuezooIconButton`
**What:** 48dp squircle icon button with shelf + spring press.

| Variant | Color | Use |
|---|---|---|
| `Confirm` | Green | Quick in-game confirm |
| `Dismiss` | Red | Close / cancel / exit game |
| `Info` | Cyan | Help overlay, hint trigger |

**Note:** There is no `Back` variant — back navigation is handled exclusively by `HuezooTopBar`'s
built-in parallelogram back button.
**Shadow:** `shapedShadow(SquircleButton, identityColor.darken())`.

---

### `GameCard`
**What:** Home screen game tile. Identity color frame, dark panel, illustration area.
**Shadow:** `shapedShadow(SquircleCard, identityColor.darken(0.5f))`.
**Use:** One card per game mode. Featured = full-width. Secondary = 2-column grid.
**Key params:** `identityColor`, `badgeText`, `triesText`, `personalBest`.
**Modifier recipe:** `rimLight()` on the inner panel for premium texture.

---

### `SkewedStatChip` — DS.7 (not yet built)
**What:** Italic-slanted container for time / score readouts in the gameplay HUD.
**Shape:** `ParallelogramBack` (same as back button — consistent design language).
**Shadow:** `shapedShadow(ParallelogramBack, identityAccent.copy(0.30f))` — same 4dp offset rule.
**Use:** Gameplay screen only — time remaining, current score, round number.
**Why skewed:** Kinetic energy. Matches the back button shape. Static rect would feel inert.

---

### `SwatchBlock`
**What:** Core gameplay tile — squircle color swatch.
**States:** Default → Pressed (0.94 scale) → Correct (green neonStrike) → Wrong (magenta shake) → Revealed.
**Sizes:** Small 80dp / Medium 120dp (default) / Large 160dp.
**Modifier recipe:** `neonStrike` on revealed outlier swatch.

---

### `DeltaEBadge`
**What:** Inline badge with ΔE value + color-coded difficulty.
**Color encoding:** ΔE > 3 → Cyan, 1–3 → Yellow, < 1 → Magenta.
**To add:** `neonStrike` at very low opacity for ΔE < 1 rounds.

---

### `RoundIndicator`
**What:** Row of dots for round progress. Pass game `identityColor`.
**States:** Inactive `SurfaceL3` → Active identity + 1.2× scale + white ring → Completed identity 70%.

---

### `ResultCard`
**What:** Share-ready summary card (ΔE hero, score, rounds, percentile).
**Use:** Result screen only. `rimLight()` on inner panel.
**Entrance:** Slide up 60dp + scale 0.9→1.0 spring.

---

### `AmbientGlow` — DS.5 (not yet built)
**What:** Two fixed radial background glows (top-left + bottom-right corners).
**Use:** Behind all screen content. Per-screen accent colors.
**Implementation:** Two `Box` composables with `colorGlow`, pointer-events blocked.

---

### `HuezooBottomSheet`
**What:** Modal sheet — `SurfaceL2` bg, 32dp top corners, custom drag handle.
**Use:** Paywall (after 5 tries), hint overlays, how-to-play. NOT for results or navigation.

---

## 7. Build Order

```
DS.3  ✅  Color tokens (SurfaceL0–L4, Background #0d0d16, M3 slot mapping)
DS.2  ✅  Modifiers: neonStrike + rimLight + depthShadow
DS.4  ✅  HuezooTopBar (frost gradient, parallelogram back button, CurrencyPill)
          + shapedShadow modifier (generalised from back button pattern)
          + ParallelogramShape in Shape.kt
DS.5      AmbientGlow composable (radial bg glows per screen)
DS.6      kineticGrid modifier (gameplay screens only)
DS.7      SkewedStatChip (reuses ParallelogramShape + shapedShadow — 0 new shape code)
DS.8      Haptics
DS.9      SwatchBlock + DeltaEBadge neonStrike polish
DS.10     Result screen sequential animation reveal
```

---

## 8. Components Still Needed

| Component | Priority | DS Phase | Notes |
|---|---|---|---|
| `AmbientGlow` | High | DS.5 | Two corner radial glows per screen |
| `KineticGrid` | Medium | DS.6 | Gameplay background texture |
| `SkewedStatChip` | Medium | DS.7 | Reuses existing `ParallelogramBack` + `shapedShadow` |
| `HuezooProgressBar` | Medium | — | Thick gradient progress bar with glow fill |
| `HuezooButton` | High | — | All 6 variants with `shapedShadow` shelf |
| `HuezooIconButton` | High | — | 3 variants (no Back variant) |
| `GameCard` | High | — | Home screen game tiles |
| `SwatchBlock` | High | — | Core gameplay tile |
| `DeltaEBadge` | High | — | ΔE readout badge |

---

---

## 9. Pending Tasks

Legend: ✅ Done · 🔧 In progress · ⬜ Not started

### DS build phases
| # | Task | Status |
|---|---|---|
| DS.3 | Color tokens (5-tier surface stack, Background #0d0d16, M3 slot mapping) | ✅ |
| DS.2 | Modifiers: `neonStrike`, `rimLight`, `depthShadow` | ✅ |
| DS.4 | `HuezooTopBar`, `ParallelogramShape`, `shapedShadow` modifier | ✅ |
| DS.5 | `AmbientGlowBackground` composable | ✅ |
| DS.7 | `SkewedStatChip` composable | ✅ |
| DS.6 | `kineticGrid` modifier (gameplay background texture) | ⬜ |
| DS.8 | Haptics | ⬜ |
| DS.9 | `neonStrike` on revealed `SwatchBlock` | ⬜ |
| DS.10 | Result screen sequential animation reveal | ⬜ |

### DS integration into existing screens
| # | Task | Status |
|---|---|---|
| I.1 | Wire `HuezooTopBar` into HomeScreen, ThresholdScreen, DailyScreen, ResultScreen | ⬜ |
| I.2 | Wrap all screens with `AmbientGlowBackground` (per-screen identity colors) | ⬜ |
| I.3 | Replace game HUD text with `SkewedStatChip` (round, tries, ΔE) | ⬜ |
| I.4 | Update `HuezooButton` to use `shapedShadow(PillShape, shelfColor)` | ⬜ |
| I.5 | Update `GameCard` to use `shapedShadow` + `rimLight` on inner panel | ⬜ |

### UX / bug fixes
| # | Task | Status |
|---|---|---|
| U.1 | HomeScreen stale data on return — fire `ScreenResumed` on lifecycle RESUME not just init | ⬜ |
| U.2 | Result screen Daily button label — show "Back to Home" instead of "Play Again" for Daily | ⬜ |
| U.3 | Nav bug: game screen stays on backstack below Result → stuck ViewModel, taps ignored | ✅ fixed in App.kt |

### Future (post-launch)
| # | Task | Status |
|---|---|---|
| F.1 | Leaderboard screen (currently stub) | ⬜ |
| F.2 | Paywall sheet + trigger after 5 attempts | ⬜ |
| F.3 | Ad reward (+1 try) | ⬜ |

---

*Stitch reference at `/Users/ksharma/Downloads/stitch/stitch/`. Screens: `screen.png` per folder.*
