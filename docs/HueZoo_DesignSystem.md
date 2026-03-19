# Hue Zoo — Game Design System
### Definitive Reference for AI-assisted Development
**Project:** `xyz.ksharma.huezoo` · Kotlin Multiplatform · Compose Multiplatform
**Last updated:** March 2026

---

## 0. What Already Exists (Don't Break These)

The project already has excellent bones. Before changing anything, understand what's there:

| File | What it does | Keep / Change |
|---|---|---|
| `HuezooColors.kt` | Neon accents on deep dark bg | ✅ Keep, add new tokens |
| `Shape.kt` → `SquircleShape` | iOS-style superellipse | ✅ Keep, it's perfect for icon buttons |
| `HuezooButton.kt` | Neo-brutalist press animation | ⚠️ Change shadow direction only |
| `GameCard.kt` | Press-into-shadow card | ⚠️ Upgrade visual style |
| `SwatchBlock.kt` | Color swatch with states | ✅ Keep, works great |
| `DeltaEBadge.kt` | Animated ΔE display | ✅ Keep |
| `RoundIndicator.kt` | Round progress dots | ✅ Keep |

**The core press animation mechanism is correct** — button translates into the shadow on press, springs back on release. Only the shadow geometry changes.

---

## 1. The Single Biggest Visual Change: Shadow Direction

This is the most important thing to fix. It changes everything.

### Current (Neo-brutalist — diagonal shadow)
```
┌──────────────┐
│   PLAY       │ ← button face
└──────────────┘
  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓  ← shadow is offset RIGHT + DOWN
```
```kotlin
// Current code in HuezooButton.kt + GameCard.kt
.offset(x = ShadowOffset, y = ShadowOffset)  // diagonal — neo-brutalist feel
```

### Target (Candy / Game — bottom shelf only)
```
┌──────────────┐
│   PLAY       │ ← button face (same color, with gloss)
└──────────────┘
▓▓▓▓▓▓▓▓▓▓▓▓▓▓   ← shelf is DIRECTLY BELOW, no X offset — candy 3D feel
```
```kotlin
// Change to:
.offset(x = 0.dp, y = ShelfHeight)  // bottom only — game candy feel
// Press animation: face translates DOWN by ShelfHeight (not diagonally)
translationX = 0f  // remove X translation
translationY = pressProgress * shelfHeightPx  // only Y
```

This one change — removing the X offset — transforms the entire feel from "indie brutalist" to "polished mobile game."

---

## 2. Color System (Extend Existing)

Keep all existing `HuezooColors`. Add these new tokens:

```kotlin
object HuezooColors {
    // ── Existing (keep unchanged) ──────────────────────────────────
    val Background  = Color(0xFF080810)
    val SurfaceL1   = Color(0xFF12121E)
    val SurfaceL2   = Color(0xFF1C1C2E)
    val SurfaceL3   = Color(0xFF26263A)
    val AccentCyan     = Color(0xFF00E5FF)
    val AccentMagenta  = Color(0xFFFF2D78)
    val AccentYellow   = Color(0xFFFFE600)
    val AccentPurple   = Color(0xFF9B5DE5)
    val AccentGreen    = Color(0xFF00F5A0)
    // ... glows, game colors, text colors (all unchanged)

    // ── New game UI tokens ─────────────────────────────────────────

    // Shelf colors (bottom shadow — darker version of each accent)
    val ShelfCyan    = Color(0xFF009DB3)
    val ShelfMagenta = Color(0xFFB3004E)
    val ShelfYellow  = Color(0xFFB8A000)
    val ShelfGreen   = Color(0xFF00A86A)
    val ShelfPurple  = Color(0xFF6B3DAD)

    // Game-action button colors
    val ActionConfirm = Color(0xFF22C55E)   // green — ✓ / confirm / correct
    val ShelfConfirm  = Color(0xFF15803D)
    val ActionDismiss = Color(0xFFEF4444)   // red — X / dismiss / wrong
    val ShelfDismiss  = Color(0xFFB91C1C)
    val ActionTry     = Color(0xFF3B82F6)   // blue — TRY / secondary
    val ShelfTry      = Color(0xFF1D4ED8)

    // Purchase / price button
    val PriceGreen    = Color(0xFF4ADE80)   // bright green for price CTA
    val ShelfPrice    = Color(0xFF16A34A)

    // Card frame (white-ish border on dark tiles — Brawl Stars style)
    val TileBorder    = Color(0xFFE2E8F0)   // near-white border on reward tiles
    val TileSurface   = Color(0xFF1E3A5F)   // deep blue tile bg (Brawl Stars blue)
    val TileShelf     = Color(0xFF0F2040)   // darker blue shelf below tile

    // Currency / gem
    val GemGreen      = Color(0xFF00E676)   // for gem/currency icon tint

    // "Best Value" badge
    val BadgeBestValue = Color(0xFF22C55E)
}
```

---

## 3. Shape System (Keep + Add)

The existing `SquircleShape` is excellent. Keep all presets. Add pill shape.

```kotlin
// Keep all existing squircle presets
val SquircleSmall  = SquircleShape(exponent = 3.5f)  // chips, badges
val SquircleMedium = SquircleShape(exponent = 4f)    // icon buttons, tiles
val SquircleLarge  = SquircleShape(exponent = 5f)    // cards
val SquircleCard   = SquircleShape(exponent = 4f)
val SquircleButton = SquircleShape(exponent = 4f)

// Add: full pill for text buttons and price buttons
val PillShape = RoundedCornerShape(50)               // fully rounded — candy buttons
```

**Shape usage guide:**
- `PillShape` → HuezooButton (text buttons), PriceButton
- `SquircleMedium` → Icon action buttons (X, ✓, info, back)
- `SquircleLarge` → GameCard
- `SquircleSmall` → Badges, currency pill, small tags

---

## 4. Components — What to Change, What to Add

---

### 4.1 HuezooButton — UPGRADE (change shadow direction)

**Change from diagonal shadow to bottom shelf only. Add gloss. Make pill-shaped.**

```
BEFORE:
  [  PLAY NOW  ]
                  ◼ (shadow right+down)

AFTER:
  ◦ [  PLAY NOW  ] ← white gloss oval top-left (15% alpha)
  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬   shelf (bottom only, 5dp, darker color)
```

**Key spec changes:**
- Shape: `PillShape` (fully rounded) instead of `RoundedCornerShape(12.dp)`
- Shadow: offset `x=0, y=ShelfHeight` instead of `x=ShadowOffset, y=ShadowOffset`
- Shelf height: 5dp (was 4dp diagonal)
- Gloss: white `Canvas` oval at top-left, 14% alpha, ~35% of button width
- Press: translate `y` only (no X), spring back

**Variants (keep same names, update shelf colors):**

| Variant | Face color | Shelf color | Use |
|---|---|---|---|
| Primary | AccentCyan | ShelfCyan | Main CTA (Play, Continue) |
| Confirm | ActionConfirm | ShelfConfirm | Submit, correct action |
| Danger | AccentMagenta | ShelfMagenta | Destructive, wrong |
| Score | AccentYellow | ShelfYellow | Score, achievement |
| Try | ActionTry | ShelfTry | Secondary / try action |
| Ghost | Transparent | SurfaceL1 | Tertiary |

**Prompt for Claude CLI:**
```
Upgrade HuezooButton.kt in the Huezoo KMP Compose project.
Package: xyz.ksharma.huezoo.ui.components

Changes:
1. Change ButtonShape from RoundedCornerShape(12.dp) to RoundedCornerShape(50) (pill)
2. Change shadow from diagonal offset (x+y) to BOTTOM shelf only:
   - Shadow box: offset(x = 0.dp, y = ShelfHeight) where ShelfHeight = 5.dp
   - Remove ShadowOffset, add ShelfHeight = 5.dp
3. Change press animation: translationX = 0f (remove), keep translationY = pressProgress * shelfPx
4. Add a gloss overlay using Canvas inside the button face box:
   - Draw a white ellipse at top-left: center at (buttonWidth * 0.25f, buttonHeight * 0.28f)
   - Size: width = buttonWidth * 0.35f, height = buttonHeight * 0.3f
   - Color: white with alpha 0.14f
   - Use drawOval on Canvas, draw AFTER the background, BEFORE the text
5. Add new variant: Confirm (bg=Color(0xFF22C55E), shadow=Color(0xFF15803D))
   and Try (bg=Color(0xFF3B82F6), shadow=Color(0xFF1D4ED8))
6. Keep all existing animation logic (spring, tween timings) unchanged

Do NOT change: animation spec, enabled/disabled logic, leadingIcon support, padding values.
```

---

### 4.2 HuezooIconButton — NEW COMPONENT

**The game's navigation and action buttons. Replaces all boring standard back buttons.**

Inspired by Brawl Stars: red X button, green ✓ button — squircle shaped, with bottom shelf.

```
   ┌─────┐     ┌─────┐     ┌─────┐
   │  ✕  │     │  ✓  │     │  ←  │
   └─────┘     └─────┘     └─────┘
   ▓▓▓▓▓▓▓     ▓▓▓▓▓▓▓     ▓▓▓▓▓▓▓
   Dismiss    Confirm      Back
   (red)      (green)     (dark)
```

**Spec:**
- Size: 48×48dp (touch target)
- Shape: `SquircleMedium`
- Shelf: 4dp bottom only
- Icon: 22×22dp, white, from `Res.drawable.*`
- Press: same spring mechanism, Y only

**Usage rules:**
- Use `Dismiss` (X) instead of any close/cancel/back in game screens
- Use `Confirm` (✓) for correct answers, submit, done
- Use `Back` (‹) only on non-game screens (leaderboard, settings)
- NEVER use Material3 `IconButton` or `TopAppBar` back arrow in game screens

**Prompt for Claude CLI:**
```
Create HuezooIconButton.kt in xyz.ksharma.huezoo.ui.components

It's a square squircle button (48dp × 48dp) with a bottom shelf (4dp).
Uses the same press-into-shelf animation as HuezooButton (spring, Y-axis only).

enum class HuezooIconButtonVariant {
    Dismiss,   // red bg (#EF4444), shelf (#B91C1C) — for X / close / cancel
    Confirm,   // green bg (#22C55E), shelf (#15803D) — for ✓ / done / correct
    Back,      // SurfaceL2 bg, SurfaceL1 shelf — for ‹ back navigation
    Info,      // AccentCyan bg, ShelfCyan — for info / help
}

Parameters:
  variant: HuezooIconButtonVariant,
  icon: Painter,   // from painterResource(Res.drawable.*)
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentDescription: String,
  enabled: Boolean = true

Shape: SquircleMedium (exponent=4f) — use the existing SquircleShape class
Shelf: offset(x=0.dp, y=4.dp)
Press animation: translationY only, spring DampingRatioMediumBouncy

Add gloss: small white oval Canvas overlay, same as HuezooButton upgrade.

Include previews showing all 4 variants in a Row.
```

---

### 4.3 PriceButton — NEW COMPONENT

**The big green purchase CTA with price. From Brawl Stars `$24.99` button.**

```
  ╔══════════════════════╗
  ║      $24.99          ║  ← large, bold, white text on bright green
  ╚══════════════════════╝
  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓   ← dark green shelf (6dp)
```

**Spec:**
- Shape: `PillShape`
- Face: `PriceGreen` (#4ADE80)
- Shelf: `ShelfPrice` (#16A34A), height 6dp
- Text: ExtraBold, 22sp, white, price string
- Width: fills parent (use `fillMaxWidth`)
- Height: 56dp
- Gloss: large white oval top-left (same pattern, 14% alpha)
- Press: Y-axis spring

**When to use:** Paywall sheet bottom CTA, purchase confirmation

**Prompt for Claude CLI:**
```
Create PriceButton.kt in xyz.ksharma.huezoo.ui.components

A wide pill-shaped purchase CTA button showing a price string (e.g. "$2.99", "$24.99").

Parameters:
  price: String,       // e.g. "$2.99"
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true

Visual spec:
- Shape: RoundedCornerShape(50) — full pill
- Width: fillMaxWidth
- Height: 56.dp
- Face color: Color(0xFF4ADE80)
- Shelf color: Color(0xFF16A34A), height 6.dp, offset(x=0,y=6.dp)
- Text: ExtraBold, 22.sp, Color.White, centered
- Gloss: white oval Canvas overlay, top-left, 14% alpha
- Press animation: Y-axis spring only (same pattern as HuezooButton)
- Disabled: SurfaceL3 face, TextDisabled text, no shelf
```

---

### 4.4 GameCard — UPGRADE (add frame + inner panel)

**Upgrade from plain border + flat surface to layered game card with frame.**

```
BEFORE:
  ┌ ─ ─ ─ ─ ─ ─ ─ ┐  (2dp identity color border)
  │ title           │
  │ subtitle        │
  └ ─ ─ ─ ─ ─ ─ ─ ┘
    ▓▓▓▓▓ (diagonal shadow)

AFTER (Candy Crush / Brawl Stars style):
  ╔═══════════════════╗  ← outer frame (5dp, identity color)
  ║ ┌───────────────┐ ║  ← inner panel (SurfaceL1, inset 6dp)
  ║ │ [ILLUSTRATION]│ ║
  ║ │               │ ║
  ║ │ TITLE     🏷  │ ║  ← badge top-right
  ║ │ subtitle      │ ║
  ║ │ Best: ΔE 1.2  │ ║
  ║ └───────────────┘ ║
  ╚═══════════════════╝
  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓  ← shelf (bottom only, 8dp, identity color darkened)
```

**Key changes:**
- Shadow: diagonal → bottom shelf only (`x=0, y=8.dp`)
- Add outer frame: `Box` filled with `identityColor`, corner radius 22dp
- Inner panel: `Box` with `SurfaceL1`, margin 5dp from frame edges, corner radius 17dp
- Illustration area: 90dp tall, centered, uses `Res.drawable.*` vector

**Prompt for Claude CLI:**
```
Upgrade GameCard.kt in xyz.ksharma.huezoo.ui.components

Structural change — layered card with visible frame:

1. SHADOW: Change from offset(x=CardShadowOffset, y=CardShadowOffset) to
   offset(x=0.dp, y=CardShelfHeight) where CardShelfHeight=8.dp
   Shadow color = identityColor.copy(alpha=0.8f) darkened (multiply RGB by 0.6)
   Press animation: translationY only (translationX = 0f always)

2. OUTER FRAME: The card background is now identityColor itself (not a border).
   Outer shape: SquircleLarge (exponent=5f), filled with identityColor

3. INNER PANEL: Inside the outer frame, a Box with:
   - 5.dp padding on all sides from the frame edges
   - Background: HuezooColors.SurfaceL1
   - Shape: SquircleShape(exponent=5f) matching outer but slightly smaller (effectively 17dp visual radius)
   - All content (illustration, title, subtitle, badge, personalBest) lives inside this inner panel

4. ILLUSTRATION AREA: Inside inner panel, a Box at the top:
   - Height: 90.dp, fillMaxWidth
   - Background: identityColor.copy(alpha=0.12f)
   - Content: centered Image(painter = illustrationPainter, ...) at 72×72dp
   - Add illustrationPainter: Painter? parameter (nullable — falls back to colored band if null)

5. BADGE: Move to top-right of the illustration area (overlay, not in text row)
   Using Box(Alignment.TopEnd) with padding 8.dp

Keep all existing parameters. Add: illustrationPainter: Painter? = null
Keep all animation logic unchanged (just fix the direction).
```

---

### 4.5 RewardTile — NEW COMPONENT

**Brawl Stars style reward/item tile. Used for game modes, rewards, shop items.**

```
  ╔═══════════╗  ← white/light border (2dp, TileBorder)
  ║           ║
  ║  [IMAGE]  ║  ← illustration or colored content
  ║           ║
  ║  🔒 15   ║  ← lock icon + value label at bottom
  ╚═══════════╝
  ▓▓▓▓▓▓▓▓▓▓▓▓  ← shelf (TileShelf, 4dp, bottom only)
```

**Spec:**
- Size: flexible, typically 80×90dp
- Shape: `SquircleMedium`
- Background: `TileSurface` (#1E3A5F — deep Brawl Stars blue)
- Border: 2dp `TileBorder` (near-white)
- Shelf: `TileShelf` (#0F2040), 4dp bottom only
- Content area: full bleed illustration up top
- Bottom label: centered, small, `TextPrimary`
- Lock state: lock icon overlay, reduced opacity (0.5f)
- Claimed state: green ✓ overlay badge top-right

**Prompt for Claude CLI:**
```
Create RewardTile.kt in xyz.ksharma.huezoo.ui.components

A square-ish tile for displaying rewards, game modes, or shop items.
Inspired by Brawl Stars reward grid tiles.

Parameters:
  illustration: Painter,
  label: String,
  modifier: Modifier = Modifier,
  isLocked: Boolean = false,
  isClaimed: Boolean = false,
  value: String? = null,      // e.g. "15", "500", "x2"
  onClick: (() -> Unit)? = null

Visual spec:
- Shape: SquircleMedium (existing SquircleShape exponent=4f)
- Background: Color(0xFF1E3A5F)
- Border: 2.dp, Color(0xFFE2E8F0) — near white
- Shelf: offset(x=0,y=4.dp), Color(0xFF0F2040), same shape, bottom only
- Illustration: Image at 48×48dp centered in top 60% of tile
- Label: small text (labelSmall), TextPrimary, bottom-centered
- If value != null: show a small currency pill below the illustration
- If isLocked: overlay semi-transparent black (40%) + lock icon (24dp, white) centered
- If isClaimed: show a small green ✓ badge (HuezooIconButton Confirm style but 24dp, no press) at top-right corner

Press animation: same shelf press pattern (Y-axis, spring) if onClick != null
No press animation if onClick is null.

Include previews: default, locked, claimed, with value.
```

---

### 4.6 CurrencyPill — NEW COMPONENT

**Small pill showing currency amount. Brawl Stars top-right gem counter.**

```
  ╭──────────────────╮
  │ 💎  512          │  ← gem icon + amount
  ╰──────────────────╯
```

**Spec:**
- Shape: `PillShape`
- Background: `SurfaceL2` (dark)
- Border: 1.5dp, `GemGreen`
- Icon: 18dp gem/circle vector from `Res.drawable.*`
- Amount: `ExtraBold`, 14sp, `TextPrimary`
- Padding: horizontal 12dp, vertical 6dp
- No shelf (this is a display element, not a button)

---

### 4.7 ProgressTracker — NEW COMPONENT

**Horizontal level/round progress bar with numbered nodes. Brawl Stars style.**

```
  ●───────●───────●───────●───────●
  7    ╔══8══╗    9       10      11
       ║ 30  ║  ← current position (gold pill)
       ╚═════╝
```

**Spec:**
- Line: 3dp height, `SurfaceL3`, full width
- Nodes: 24dp circles, `SurfaceL2` background, `TextSecondary` number
- Current node: `AccentYellow` pill (wider than tall), `Background` text, ExtraBold
- Completed nodes: `AccentGreen` fill with ✓ icon
- Animated: when moving to next node, slide the gold pill with spring

---

## 5. Game Navigation — No Boring Back Buttons

**Rule: In any game screen or modal, NEVER use standard back arrow or TopAppBar.**

Use `HuezooIconButton` instead:

```
Game screen top bar:
  ┌────────────────────────────────────┐
  │ [✕]    Round 3/6    ΔE 2.4        │
  │ Dismiss             DeltaEBadge   │
  └────────────────────────────────────┘

Result screen:
  ┌────────────────────────────────────┐
  │                               [✕] │
  │         RESULT CARD               │
  │                                   │
  │  [↩ Try Again]  [✓ Leaderboard]  │
  └────────────────────────────────────┘

Paywall sheet:
  ┌────────────────────────────────────┐
  │                               [✕] │
  │      PRO PASS content...          │
  │                                   │
  │         [$24.99]                  │
  └────────────────────────────────────┘
```

**Implementation rule:**
```kotlin
// NEVER do this in game screens:
TopAppBar(navigationIcon = { IconButton { Icon(Icons.AutoMirrored.Default.ArrowBack) } })

// ALWAYS do this instead:
Box(modifier = Modifier.fillMaxSize()) {
    // game content
    HuezooIconButton(
        variant = HuezooIconButtonVariant.Dismiss,
        icon = painterResource(Res.drawable.ic_close),
        onClick = onDismiss,
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        contentDescription = "Close"
    )
}
```

---

## 6. Typography (Upgrade)

Current: Space Grotesk (placeholder, TTF files not yet added).

**Action:** Add Space Grotesk TTF files to `composeResources/font/` and wire them up.
Alternatively, use **Nunito** for a rounder, friendlier game feel.

```
Files needed in composeResources/font/:
  space_grotesk_regular.ttf
  space_grotesk_medium.ttf
  space_grotesk_semibold.ttf
  space_grotesk_bold.ttf

OR replace with:
  nunito_regular.ttf
  nunito_semibold.ttf
  nunito_bold.ttf
  nunito_extrabold.ttf
```

**Game-specific text styles (add to typography):**

```kotlin
// Game heading — big, bold, slightly tracked
val GameHeading = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 36.sp,
    letterSpacing = (-0.5).sp,
    lineHeight = 40.sp
)

// Price text — large, prominent
val PriceText = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 22.sp,
    letterSpacing = 0.sp
)

// Score value — very large, mono-spaced feel
val ScoreValue = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 56.sp,
    letterSpacing = (-1).sp
)
```

---

## 7. Illustration & Icon System

**All icons and illustrations must use Compose Resources — never emoji, never system icons.**

```
composeResources/drawable/
├── ic_close.xml           → white X path
├── ic_check.xml           → white checkmark path
├── ic_chevron_left.xml    → white ‹ chevron
├── ic_info.xml            → white i circle
├── ic_lock.xml            → white lock icon
├── ic_gem.xml             → green gem shape (for currency)
├── ic_trophy.xml          → trophy for leaderboard
├── ic_share.xml           → share arrow
├── game_delta_eye.xml     → spectrum eye illustration
├── game_odd_swatch.xml    → 3×3 grid with odd one highlighted
├── game_mix_match.xml     → two overlapping color circles
├── game_color_memory.xml  → color swatch + clock
└── badge_checkmark.xml    → checkmark for claimed/completed overlay
```

**Vector style rules:**
- Flat fill, 2-3 color stops max
- Thick stroke outline equivalent (strokeWidth 3-4 for 24dp icons)
- No drop shadows inside the vector — the Compose shelf/glow handles depth
- All on transparent background
- ViewBox: 24×24 for icons, 80×80 for game illustrations

---

## 8. Screen Layout Patterns

### 8.1 Game Screen Template
```
┌─────────────────────────────────────┐ ← Background (#080810)
│                               [✕]  │ ← HuezooIconButton Dismiss, top-end
│  ●──●──●──●──●──●              │ ← ProgressTracker (round 3/6)
│         ▼                           │
│                                     │
│      ┌──────┐ ┌──────┐ ┌──────┐   │ ← SwatchBlock × 3 (game area)
│      │      │ │      │ │      │   │
│      └──────┘ └──────┘ └──────┘   │
│                                     │
│  ΔE 2.4 badge        10s timer bar │
│                                     │
└─────────────────────────────────────┘
```

### 8.2 Home Screen Template
```
┌─────────────────────────────────────┐
│  Hue Zoo          💎 512            │ ← app title + CurrencyPill
│                                     │
│  ┌──────────┐   ┌──────────┐       │
│  │ Game 1   │   │ Game 2   │       │ ← GameCard × 4 in 2×2 grid
│  └──────────┘   └──────────┘       │
│  ┌──────────┐   ┌──────────┐       │
│  │ Game 3   │   │ Game 4   │       │
│  └──────────┘   └──────────┘       │
│                                     │
│  [Home]    [Leaderboard]  [Profile] │ ← Navigation bar (no TopAppBar)
└─────────────────────────────────────┘
```

### 8.3 Paywall Sheet Template
```
╔═══════════════════════════════════════╗
║  PRO PASS                       [✕]  ║ ← HuezooIconButton Dismiss
║                                       ║
║  ┌─────────────┐  ┌─────────────┐   ║
║  │ [Extra      │  │ BEST VALUE  │   ║ ← RewardTile × 2 (top row)
║  │  Cosmetics] │  │ 💎 500      │   ║
║  └─────────────┘  └─────────────┘   ║
║  ┌──────────────────────────────┐    ║
║  │  x2 XP  │  ×1000 Progress   │   ║ ← RewardTile (wide, 2 col)
║  └──────────────────────────────┘    ║
║                                       ║
║  ╔══════════════════════════════╗    ║
║  ║         $24.99               ║    ║ ← PriceButton (full width)
║  ╚══════════════════════════════╝    ║
╚═══════════════════════════════════════╝
```

---

## 9. Animation Tokens

```kotlin
object HuezooAnimation {
    // Shelf press (all interactive components)
    val PressDown = tween<Float>(durationMillis = 80)
    val PressRelease = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    // Shelf heights by component
    val ButtonShelf  = 5.dp
    val CardShelf    = 8.dp
    val IconBtnShelf = 4.dp
    val PriceShelf   = 6.dp
    val TileShelf    = 4.dp

    // Entrance
    val StaggerDelay = 80          // ms, between grid items
    val EntranceScale = 0.85f      // starting scale for card entrances

    // Swatch feedback
    val ShakeDistance = 10.dp
    val ShakeCycles   = 3
    val ShakeDuration = 300        // ms

    // Score
    val ScoreCountUp = 800         // ms, with EaseOutCubic

    // Gloss (static, not animated)
    val GlossAlpha = 0.14f
}
```

---

## 10. What NOT to Do

```
❌ NEVER use TopAppBar with back arrow in game screens
✅ Use HuezooIconButton(variant = Back / Dismiss) instead

❌ NEVER use diagonal shadow (x+y offset)
✅ Use bottom shelf only (x=0, y=shelfHeight)

❌ NEVER use emoji as UI elements (✅ ❌ 💎)
✅ Use Res.drawable.* vector composables

❌ NEVER use Material3 Card {} for game cards
✅ Use layered Box with outer frame + inner panel

❌ NEVER use light theme — Huezoo is dark only

❌ NEVER use standard Material3 Button for purchase CTAs
✅ Use PriceButton composable

❌ NEVER use system back gesture as only navigation in game screens
✅ Always provide visible HuezooIconButton Dismiss in game screens

❌ NEVER use Modifier.shadow() for depth effects
✅ Use the shelf Box pattern (offset, fixed behind face)
```

---

## 11. Build Order for Claude CLI

Run these prompts in order. Each builds on the previous.

```
Step 1: Font setup
→ "Add Space Grotesk (or Nunito) font files to composeResources/font/
   and wire them in Typography.kt for the Huezoo KMP project."

Step 2: Color tokens
→ Use the new HuezooColors additions from Section 2 above.
   Add to existing Color.kt — do not replace existing tokens.

Step 3: HuezooButton upgrade
→ Use Prompt from Section 4.1

Step 4: HuezooIconButton (new)
→ Use Prompt from Section 4.2

Step 5: PriceButton (new)
→ Use Prompt from Section 4.3

Step 6: GameCard upgrade
→ Use Prompt from Section 4.4

Step 7: RewardTile (new)
→ Use Prompt from Section 4.5

Step 8: CurrencyPill (new)
→ "Create CurrencyPill.kt in xyz.ksharma.huezoo.ui.components.
   A pill-shaped display (not interactive) showing a gem icon + amount.
   Shape: RoundedCornerShape(50). Background: SurfaceL2. Border: 1.5dp GemGreen.
   Parameters: amount: Int, icon: Painter. No shelf, no press animation."

Step 9: ProgressTracker (new)
→ "Create ProgressTracker.kt in xyz.ksharma.huezoo.ui.components.
   Horizontal row of numbered round indicators connected by a line.
   Current round shown as a gold pill (AccentYellow background, Bold text).
   Completed rounds shown with AccentGreen fill + checkmark.
   Upcoming rounds shown as SurfaceL3 circles with TextSecondary numbers.
   Scrollable horizontally if more than 6 rounds visible."

Step 10: Vector drawables
→ "Create the vector XML files listed in Section 7 in
   composeApp/src/commonMain/composeResources/drawable/"

Step 11: HomeScreen
→ "Build HomeScreen.kt using the 2×2 GameCard grid with CurrencyPill header,
   staggered entrance animation (80ms delay per card), and no TopAppBar."
```

---

## 12. File Reference

```
composeApp/src/commonMain/
├── composeResources/
│   ├── drawable/          ← all vectors go here
│   └── font/              ← Space Grotesk / Nunito TTF files
└── kotlin/xyz/ksharma/huezoo/
    ├── ui/theme/
    │   ├── Color.kt       ← extend with new shelf/action tokens
    │   ├── Shape.kt       ← add PillShape
    │   ├── Typography.kt  ← add font + game text styles
    │   └── Modifiers.kt   ← keep as-is
    └── ui/components/
        ├── HuezooButton.kt      ← upgrade (shelf direction + pill + gloss)
        ├── HuezooIconButton.kt  ← NEW (X, ✓, back, info)
        ├── PriceButton.kt       ← NEW ($24.99 purchase CTA)
        ├── GameCard.kt          ← upgrade (frame + inner panel + shelf)
        ├── RewardTile.kt        ← NEW (Brawl Stars reward tile)
        ├── CurrencyPill.kt      ← NEW (gem + amount display)
        ├── ProgressTracker.kt   ← NEW (round/level progress)
        ├── SwatchBlock.kt       ← keep as-is ✅
        ├── DeltaEBadge.kt       ← keep as-is ✅
        ├── RoundIndicator.kt    ← keep as-is ✅
        └── HuezooBottomSheet.kt ← keep as-is ✅
```
