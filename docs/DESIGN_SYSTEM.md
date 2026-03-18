# Huezoo — Design System
*Game-grade UI. Not a settings screen. Not Material 3 defaults.*

---

## Design Philosophy

> Every tap should feel like something happened.
> Every correct answer should feel satisfying.
> Every wrong answer should sting — politely, but deeply.

The UI is dark, vibrant, tactile. Buttons have depth. Colors glow.
Swatches feel physical — like chips you can pick up.
Sound and haptics are baked in from day one, not bolted on.

---

## The Sting Principle — Core UX Rule

**This is the most important copy and UX rule in the entire app. Never forget it.**

Wrong answers don't say "Oops! Try again :)" — that's a toddler app.

Wrong answers tease. They challenge. They make the player feel like they *almost* had it
and now they *have* to go again. The tone is a confident friend who just beat you at chess
and is already setting up the board again.

> Polite enough that it doesn't feel mean.
> Sharp enough that you can't just close the app.

### The Emotion We're Designing For

Not shame. Not frustration. **Determination.**

The player should think: *"That's not fair, I can do better than that."*
Then immediately tap Play Again.

### Rules for All In-App Copy

1. **Never say sorry.** The app never apologizes for the player losing. That's condescending.
2. **Never use exclamation marks on wrong answers.** "Wrong!" feels aggressive. "That wasn't it." feels like a raised eyebrow.
3. **Always leave a door open.** Every loss message implies the next attempt is closer.
4. **Reference their specific failure.** "ΔE 2.4 got you" is more stinging than generic "You missed."
5. **Tease the ceiling.** Show them what's possible — make them feel the gap between where they are and where they could be.
6. **On wins, be cool not hype.** Don't over-celebrate easy wins. Make hard wins feel genuinely earned.

### Copy Tone by Moment

| Moment | Tone | Feel |
|---|---|---|
| Game start | Confident dare | "Let's see what you've got." |
| Correct (easy) | Understated | "Not bad." |
| Correct (hard) | Genuine respect | "Okay. That one was real." |
| Wrong (early round) | Light tease | "ΔE 3.1 already? Hmm." |
| Wrong (deep in game) | Real sting | "You were at ΔE 1.4. So close it hurts." |
| New personal best | Cool, not hype | "ΔE 1.1. That's actually impressive." |
| No improvement | Dare | "Same as last time. You sure that's your limit?" |
| Daily Challenge done | Matter-of-fact | "Day 47. Done. Come back tomorrow." |
| Out of tries (paywall) | Wry | "5 tries. Still not satisfied? Respect." |
| Top of leaderboard | Rare praise | "ΔE 0.9. Most people can't even see that difference." |

### Example Wrong Answer Copy Pool

Rotate these so players don't see the same message twice in a row.

**Early miss (ΔE was easy, ~3.0+):**
- "That one wasn't even close."
- "ΔE 3.1. Your eyes were off today."
- "Warmer. Not warm enough."

**Mid-game miss (ΔE ~1.5–3.0):**
- "ΔE {X} got you. Most people tap out here."
- "That gap was real. You almost saw it."
- "Your eyes made it to round {N}. They can go further."

**Deep miss (ΔE < 1.5 — genuinely hard):**
- "ΔE {X}. That's sub-pixel territory. Seriously impressive."
- "You were at ΔE {X}. Only {Y}% of players ever get there."
- "ΔE {X}. The difference was barely real. You still missed it."

**Same score as personal best:**
- "ΔE {X}. Again. You're stuck here. Break it."
- "Your ceiling is ΔE {X}. Or is it?"

**Improvement:**
- "ΔE {X}. You moved the line. Now move it again."
- "Closer. Not there yet."

### Example Game Start / Home Copy

- App tagline: **"How sharp are your eyes?"**
- The Threshold subtitle: **"One miss. That's all it takes."**
- Daily Challenge subtitle: **"Everyone gets the same puzzle. Not everyone survives it."**
- Leaderboard header: **"The sharpest eyes in the room."**
- Empty leaderboard: **"No one's submitted yet. Could be you at #1."**

---

---

## Visual Language

### Dark Game Aesthetic
- Near-black background with a subtle cool tint — feels like a studio, not a form
- Cards float above the background with layered elevation shadows
- Accent colors are electric, not pastel
- Everything rounded — squircle shapes (superellipse), never hard corners

### Color Palette

```
Background:       #080810   (deep space black, slight blue)
Surface L1:       #12121E   (cards, game areas)
Surface L2:       #1C1C2E   (elevated elements, sheets)
Surface L3:       #26263A   (pressed states, selected)

Accent Cyan:      #00E5FF   (primary interactive, correct)
Accent Magenta:   #FF2D78   (wrong, danger, streak)
Accent Yellow:    #FFE600   (score, highlight, Daily)
Accent Purple:    #9B5DE5   (Threshold game identity)
Accent Green:     #00F5A0   (success, streak complete)

Text Primary:     #FFFFFF
Text Secondary:   #8888AA
Text Disabled:    #44445A

Glow Cyan:        #00E5FF40  (use as shadow color for glow effect)
Glow Magenta:     #FF2D7840
Glow Yellow:      #FFE60040
```

### Typography

Use **Space Grotesk** (free, Google Fonts) — rounded, technical, game-feel.
Fallback: system default rounded sans.

```
Display:    Space Grotesk Bold,    48sp  — score numbers, ΔE values
Headline:   Space Grotesk SemiBold, 28sp  — game titles
Title:      Space Grotesk SemiBold, 20sp  — card titles
Body:       Space Grotesk Regular,  16sp  — descriptions
Label:      Space Grotesk Medium,   13sp  — badges, tags
Mono:       monospace (system),     14sp  — ΔE decimal values
```

---

## Shapes

### Squircle (superellipse)
All cards, buttons, swatches use squircle corners — softer than rounded rect,
more distinctive than a circle. Achieved via `GenericShape` in Compose with
a superellipse path formula.

```kotlin
// Corner radius as % of size — not fixed dp
Small swatch:   24% radius  (feels like a soft tile)
Medium swatch:  22% radius
Large swatch:   20% radius
Game cards:     28dp corner radius (squircle path)
Buttons:        Full squircle (pill-ish but squarish)
Bottom sheets:  32dp top corners only
Result card:    40dp all corners
```

### Swatch Shape Variants
- **Tile** — equal width/height, squircle. Used in grids.
- **Pill** — wide, short. Used for color comparison strips.
- **Dot** — small circle. Used in round indicators.

---

## Components

### `ChromaButton` — Primary Game Button
NOT a Material Button. Custom drawn.

**Visual anatomy:**
- Background: gradient fill (angle 135°, accent color → slightly darker)
- Top edge: 1dp lighter highlight line (feels 3D)
- Bottom edge: 3dp darker shadow band (gives depth/lift)
- Label: bold, white, center
- Outer glow: blurred shadow in accent color

**States:**
```
Default:   full gradient + bottom shadow (lifted)
Pressed:   scale 0.94, shadow disappears (pushed down), haptic: light
Disabled:  Surface L2 fill, text dimmed, no glow
Loading:   shimmer overlay on gradient
```

**Variants:**
```
Primary    — accent cyan gradient, used for main CTA
Danger     — magenta gradient, used for destructive actions
Ghost      — transparent fill, accent-colored border + text
Score      — yellow gradient, used for "Submit Score" only
```

---

### `SwatchBlock` — The Core Game Element
The colored tile players tap. Must feel tactile and physical.

**Visual anatomy:**
- Fill: the game color
- Border: 2dp inner highlight (lighter tint of fill color) on top-left
- Border: 2dp shadow (darker tint of fill color) on bottom-right
- Subtle gradient overlay: linear, light-to-transparent (top-left → bottom-right)
- This combo makes it look like a physical color chip under studio lighting

**States:**
```
Default:    resting, slight scale spring on appear (0.85 → 1.0)
Hover:      scale 1.04 (pointer devices)
Pressed:    scale 0.92 instantly, haptic: light
Correct:    green border pulse, scale 1.08 → 1.0, haptic: success
Wrong:      shake X ±10dp 3 cycles, magenta border flash, haptic: error
Revealed:   scale 1.0, glowing border in accent green
```

---

### `TimerBar`
Animated progress bar showing time remaining.

- Shape: pill, full width, 8dp tall
- Fill: gradient shifts as time runs out:
  ```
  100% → 60%:  Cyan    (#00E5FF)
   60% → 30%:  Yellow  (#FFE600)
   30% →  0%:  Magenta (#FF2D78)
  ```
- Background track: Surface L2
- When below 30%: subtle pulse animation (opacity 1.0 ↔ 0.7, 500ms)
- When below 10%: haptic tick every second

---

### `DeltaEBadge`
Shows the ΔE value after each round.

- Shape: squircle pill
- Background: Surface L2 with accent glow
- Text: monospace, Display size, color reflects difficulty:
  ```
  ΔE > 3.0:   Cyan   (easy)
  ΔE 1.5–3.0: Yellow (medium)
  ΔE < 1.5:   Magenta (hard)
  ```
- Appears with: scale 0.6 → 1.0 + fade in, spring 400ms

---

### `GameCard` — Home Screen Card
The entry point to each game.

- Background: Surface L1 with subtle gradient from game's identity color (10% opacity)
- Left accent bar: 4dp wide, game identity color, full height
- Title: Headline type, white
- Description: Body type, Text Secondary
- Personal best: Label type + ΔE badge or score chip
- Identity colors per game:
  ```
  The Threshold:       Purple  (#9B5DE5)
  Daily Challenge:     Yellow  (#FFE600)
  What's My Delta E?:  Cyan    (#00E5FF)
  Odd Swatch Out:      Green   (#00F5A0)
  Mix to Match:        Magenta (#FF2D78)
  Color Memory:        Orange  (#FF9500)
  ```
- Press: scale 0.97, shadow reduces, haptic: light
- Identity color glow on card edge when pressed

---

### `ResultCard`
The end-of-game score card. Also what gets screenshotted for sharing.

- Background: radial gradient — game identity color at 15% opacity from center
- Large ΔE or score number: Display type, glowing in identity color
- Subtitle: "Better than X% of players" — Title type
- Stat rows: Label type, divider lines in Surface L3
- Confetti: 50 particles, mix of identity color + white + accent
- Card border: 1.5dp, identity color at 40% opacity
- Share image: 1:1 ratio, 800×800px render

---

### `RoundIndicator`
Row of dots showing game progress.

- Inactive dot: 8dp circle, Surface L3
- Active dot: 14dp squircle, accent color, glow
- Completed dot: 8dp circle, accent green, filled
- Transition: spring scale animation between states

---

### `BottomSheet` — Paywall / Name Entry
- Top handle bar: pill, Surface L3
- Background: Surface L2, top corners 32dp squircle
- Title: Headline, center
- Content: flexible slot
- Action buttons: ChromaButton variants, full width

---

## Haptics

Platform-specific via `expect`/`actual` in KMP.

```kotlin
expect fun haptic(type: HapticType)

enum class HapticType {
    Light,       // tap, navigation, swatch appear
    Medium,      // round transition, submit
    Heavy,       // game start, game over
    Success,     // correct answer — double light pulse pattern
    Error,       // wrong answer — short heavy buzz
    Warning,     // timer low (below 10%)
    Selection,   // slider drag tick
}
```

**Android:** `VibrationEffect` via `Vibrator` / `VibratorManager`
**iOS:** `UIImpactFeedbackGenerator` / `UINotificationFeedbackGenerator`

---

## Sound

Short, non-intrusive. All sounds < 200ms except level-up chime.
Volume respects system silent mode — sound is OFF by default, user opt-in.

```
correct.wav      — soft ascending two-tone (C → E), 120ms
wrong.wav        — descending tone, slightly dissonant, 180ms
levelup.wav      — ascending chime (3 notes), 400ms
gameover.wav     — descending minor chord, 350ms
tick.wav         — single soft click, 30ms (timer warning)
confetti.wav     — sparkle shimmer sound, 600ms (result screen)
button_tap.wav   — subtle soft click, 40ms
swatch_tap.wav   — soft thud, 50ms
```

**KMP approach:**
```kotlin
expect fun playSound(sound: SoundEffect)

enum class SoundEffect {
    Correct, Wrong, LevelUp, GameOver,
    Tick, Confetti, ButtonTap, SwatchTap
}
```

**Android:** `SoundPool` (low latency, pre-loaded)
**iOS:** `AVAudioPlayer` or `SystemSoundID` for shortest sounds

---

## Animation Spec

| Trigger | Animation | Easing | Duration |
|---|---|---|---|
| Screen enter | Fade + slide up 24dp | Spring (stiffness 300) | 350ms |
| Home cards load | Stagger fade-up, 80ms delay per card | EaseOutCubic | 300ms each |
| Swatch appear | Scale 0.8 → 1.0 | Spring (stiffness 400, damp 0.7) | 250ms |
| Swatch correct | Scale 1.0 → 1.1 → 1.0 + green border flash | Spring | 300ms |
| Swatch wrong | ShakeX ±10dp × 3 | Linear | 300ms total |
| Timer bar drain | Width lerp | Linear | duration of round |
| Timer pulse (30%) | Opacity 1.0 ↔ 0.65 | EaseInOut | 500ms loop |
| ΔE badge appear | Scale 0.5 → 1.0 + fade | Spring (stiffness 500) | 400ms |
| Score count-up | Int lerp 0 → final | Spring (stiffness 200) | 800ms |
| Result card in | Slide up 60dp + scale 0.9 → 1.0 | Spring (stiffness 280) | 450ms |
| Confetti | 50 particles, gravity 0.4, spin random | Physics | 2000ms |
| Button press | Scale 1.0 → 0.94 | Snap | 80ms |
| Button release | Scale 0.94 → 1.0 | Spring (stiffness 600) | 200ms |
| Game card press | Scale 1.0 → 0.97 | Snap | 80ms |

---

## Game Identity Colors

Each game has an identity color used as: card accent bar, card background tint,
ΔE badge color, confetti color, glow color on result card.

| Game | Identity Color | Hex |
|---|---|---|
| The Threshold | Electric Purple | `#9B5DE5` |
| Daily Challenge | Electric Yellow | `#FFE600` |
| What's My Delta E? | Cyan | `#00E5FF` |
| Odd Swatch Out | Electric Green | `#00F5A0` |
| Mix to Match | Hot Magenta | `#FF2D78` |
| Color Memory | Electric Orange | `#FF9500` |

---

## Implementation Notes

### Glow Effect (Compose)
```kotlin
// Apply to any Modifier
fun Modifier.colorGlow(color: Color, radius: Dp = 16.dp) = this.then(
    Modifier.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    this.color = Color.Transparent.toArgb()
                    setShadowLayer(radius.toPx(), 0f, 0f, color.copy(alpha = 0.6f).toArgb())
                }
            }
            canvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                16.dp.toPx(), 16.dp.toPx(), paint
            )
        }
    }
)
```

### 3D Button Depth Effect
Achieved via layered `Box`:
1. Bottom layer: darker color, offset 3dp down — the "shadow band"
2. Top layer: gradient fill — the button surface
3. Top edge overlay: 1dp lighter line — the highlight

### Squircle Shape
```kotlin
val SquircleShape = GenericShape { size, _ ->
    val n = 4f  // superellipse exponent
    val w = size.width
    val h = size.height
    // approximate with bezier curves for performance
    // or use RoundedCornerShape(28%) as close approximation
}
```

---

## Task List: Design System

### Phase DS.0 — Foundation
- [ ] DS.0.1 Define `ChromaColors.kt` — all color tokens as `Color` constants
- [ ] DS.0.2 Define `ChromaTypography.kt` — Space Grotesk font loading (expect/actual for each platform), all text styles
- [ ] DS.0.3 Define `ChromaTheme.kt` — `MaterialTheme` wrapper with custom colors + typography
- [ ] DS.0.4 Add Space Grotesk font files to Android assets + iOS bundle

### Phase DS.1 — Shapes & Effects
- [ ] DS.1.1 `SquircleShape` — GenericShape implementation
- [ ] DS.1.2 `Modifier.colorGlow()` — BlurMaskFilter glow extension
- [ ] DS.1.3 `Modifier.depthShadow()` — layered shadow for 3D card feel
- [ ] DS.1.4 `SwatchGradientOverlay` — highlight + shadow overlay for physical chip look

### Phase DS.2 — Core Components
- [ ] DS.2.1 `ChromaButton` — primary, danger, ghost, score variants with 3D press
- [ ] DS.2.2 `SwatchBlock` — sizes sm/md/lg, all states (default, pressed, correct, wrong)
- [ ] DS.2.3 `GameCard` — identity color, personal best, press animation
- [ ] DS.2.4 `TimerBar` — color-shifting animated countdown
- [ ] DS.2.5 `DeltaEBadge` — difficulty-colored ΔE label
- [ ] DS.2.6 `RoundIndicator` — dot row, animated state transitions
- [ ] DS.2.7 `ResultCard` — radial gradient, glow border, share-ready layout
- [ ] DS.2.8 `ChromaBottomSheet` — game-styled sheet

### Phase DS.3 — Haptics
- [ ] DS.3.1 `HapticType` enum in commonMain
- [ ] DS.3.2 `expect fun haptic(type: HapticType)` declaration
- [ ] DS.3.3 Android `actual` — VibrationEffect patterns per type
- [ ] DS.3.4 iOS `actual` — UIImpactFeedbackGenerator / UINotificationFeedbackGenerator

### Phase DS.4 — Sound
- [ ] DS.4.1 Source/create 8 sound files (correct, wrong, levelup, gameover, tick, confetti, button_tap, swatch_tap)
- [ ] DS.4.2 `SoundEffect` enum in commonMain
- [ ] DS.4.3 `expect fun playSound(sound: SoundEffect)` declaration
- [ ] DS.4.4 Android `actual` — SoundPool, pre-load all sounds on init
- [ ] DS.4.5 iOS `actual` — AVAudioPlayer
- [ ] DS.4.6 User setting: sound on/off (default off, stored in SQLDelight)
- [ ] DS.4.7 Respect system silent mode on both platforms

### Phase DS.5 — Animation Utilities
- [ ] DS.5.1 `shakeAnimation()` — reusable shake modifier
- [ ] DS.5.2 `confettiEffect()` — particle system composable
- [ ] DS.5.3 `countUpAnimation()` — animates Int/Float from 0 to target
- [ ] DS.5.4 `staggeredEntrance()` — list entrance with configurable delay
