# Huezoo — Design System
*Game-grade UI. Not a settings screen. Not Material 3 defaults.*

---

## Design Philosophy

> Every tap should feel like something happened.
> Every correct answer should feel satisfying.
> Every wrong answer should sting — politely, but deeply.

The UI is dark, vibrant, tactile. Buttons have depth. Colors glow.
Swatches feel physical — like chips you can pick up.

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

**Early miss (ΔE ~3.0+):**
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

### Home Screen Copy

- App tagline: **"How sharp are your eyes?"**
- The Threshold subtitle: **"One miss. That's all it takes."**
- Daily Challenge subtitle: **"Everyone gets the same puzzle. Not everyone survives it."**
- Leaderboard header: **"The sharpest eyes in the room."**

---

## Game Aesthetic Reference

> The target feel: **premium mobile game — bold, physical, alive.**
> Reference: hero-card screens from games like Brawl Stars, Hero Hunters, Overdrive City.
> Every screen should feel like something a player is proud to screenshot.

### The Core Principle — Everything Bigger

In mobile games, UI elements are always slightly larger than you think is right. If a font size feels "a little too big" it's probably correct. If a button looks "a little chunky" it's right. Players tap fast, glance at numbers while playing, read stat labels in a second. Everything must be readable at a glance.

```
Minimum touch target:    48dp × 48dp  (never smaller)
Primary button height:   56dp  (pill shape, full width CTAs)
Card minimum height:     160dp
Card title font:         Fredoka Bold, 22sp minimum — not 16sp like a settings screen
Number/stat font:        Antonio Bold — oversized, it's the hero of every readout
Label font:              Space Grotesk Medium, 13sp minimum, UPPERCASE where possible
```

### Visual Language

**Backgrounds:** Dark, deep. The app background is `#080810`. On home and result screens, let the identity color breathe at low opacity (~8-12%) as a radial gradient from center. On game screens, keep it neutral dark so color accuracy isn't compromised.

**Cards:** Solid dark panel (`SurfaceL2`) floating inside a colored frame (identity color, 5dp inset). The shelf below the card is the shelf color — darker version of the identity color. The card feels physical, like a trading card you could pick up.

**Buttons:** Chunky and confident. The face color is a solid saturated accent, the text is always `.onColor` computed (never hardcoded white or black). The shelf is the same color darkened. On press, the face slides into the shelf — immediate, spring-back on release.

**Numbers:** Antonio Bold, always. Numbers in a game app are the scoreboard. They should be the most visible thing on the screen. Never let them compete with the label text — the number wins.

**Icons:** Vector only — no emoji. Icons on buttons use the button's `.onColor` as their tint. Icons in the UI (gem, back arrow, info) are white on dark surfaces, always.

**Character illustrations:** AI-generated per game mode. Style prompt for Gemini / Midjourney:
```
cartoon game character, vibrant saturated colors, mobile game art style,
bold clean outlines, 3D render feel, [game-specific theme], no background,
transparent PNG, hero pose
```
Each game mode gets its own character in the GameCard illustration slot (90dp height area).
Replace `[game-specific theme]` per game:
- The Threshold → `color scientist with magnifying glass examining swatches`
- Daily Challenge → `calendar knight with colorful shield`
- Color Memory → `wizard with glowing color orbs`

---

## Color Palette

```
── Surfaces (always dark — static, do not change with system theme) ──────────
Background:       #080810   deep space — used as inner panel of result/game cards
Surface L1:       #12121E   card face, game board
Surface L2:       #1C1C2E   elevated panels, sheets, inner card panels
Surface L3:       #26263A   pressed states, separators

── Accents (electric — for interactive elements, identity colors, glows) ─────
Accent Cyan:      #00E5FF   primary CTA, "correct", The Threshold active
Accent Magenta:   #FF2D78   wrong answer, danger, dismiss
Accent Yellow:    #FFE600   score display, Daily Challenge identity
Accent Purple:    #9B5DE5   The Threshold game identity
Accent Green:     #00F5A0   success, streak complete, ActionConfirm
Warm Orange:      #FF8A50   Daily Challenge, secondary warm accent

── Text (always on dark surfaces — use these, not MaterialTheme.colorScheme) ─
Text Primary:     #FFFFFF   all primary text on dark cards/panels
Text Secondary:   #9898BB   subtitles, metadata, secondary labels (~5.5:1 on L1)
Text Disabled:    #777799   personal best, helper labels, faded states (~4.5:1 on L1)

── Glows (40% alpha of accent — shadows, borders, indicator rings) ───────────
Glow Cyan:        #00E5FF40
Glow Magenta:     #FF2D7840
Glow Yellow:      #FFE60040
Glow Purple:      #9B5DE540
```

**Critical rule for text color on component surfaces:**
`HuezooColors.SurfaceL1/L2/Background` are static dark tokens — they do NOT respond to the system light/dark theme. Always use `HuezooColors.TextPrimary / TextSecondary / TextDisabled` explicitly for text on these surfaces. Never use `MaterialTheme.colorScheme.onBackground` for text inside cards or buttons — that value goes near-black in light mode and becomes invisible on the dark static surface.

```kotlin
// ✅ Correct — explicit static token, always right on SurfaceL2
HuezooTitleMedium(text = title, color = HuezooColors.TextPrimary)
HuezooBodyMedium(text = subtitle, color = HuezooColors.TextSecondary)

// ❌ Wrong — theme-aware, breaks on light mode when card surface stays dark
HuezooTitleMedium(text = title)  // defaults to MaterialTheme.colorScheme.onBackground
```

For text on **colored backgrounds** (button face, badge, identity-color chip): use `.onColor` from `ColorExt.kt`.

---

## Typography

Huezoo uses a **three-font system** — each font has a deliberate role. Never pick fonts ad-hoc.

| Font | Role | Used for |
|---|---|---|
| **Antonio** | Bold, condensed display | Numbers, scores, ΔE values, hero text, app name |
| **Fredoka** | Round, friendly | Card titles, section headings, playful labels |
| **Space Grotesk** | Geometric sans | Body copy, button labels, metadata, badges |

> Full scale, composables, color defaults, and rules: see **`docs/TYPOGRAPHY.md`**

#### Quick scale reference

```
displayLarge   Antonio Bold      56sp   ΔE hero (ResultCard), big score reveals
displayMedium  Antonio Bold      40sp   SCORE / ROUNDS stats (ResultCard)
displaySmall   Antonio Bold      28sp   DeltaEBadge number, compact numeric readout
headlineLarge  Antonio Bold      40sp   App name "Hue Zoo", screen hero title
headlineSmall  Antonio Medium    20sp   Currency pill amount, inline numeric labels
headlineMedium Fredoka SemiBold  28sp   Section headings, dialog titles
titleLarge     Fredoka Bold      22sp   Card headers, sheet titles
titleMedium    Fredoka SemiBold  20sp   GameCard title, bottom sheet heading
titleSmall     Fredoka Regular   16sp   Secondary titles, list item sub-headers
bodyLarge      Space Grotesk     18sp   Long-form description text
bodyMedium     Space Grotesk     16sp   GameCard subtitle, description
bodySmall      Space Grotesk     14sp   Captions, helper text
labelLarge     Space Grotesk Bold   16sp   Button labels, prominent chips
labelMedium    Space Grotesk Medium 13sp   Secondary labels
labelSmall     Space Grotesk Medium 12sp   Badges, tries text, personal best, stat headers
```

**Never use bare `Text()` composable anywhere in the app.**
Always use the typed `HuezooText` variants from `HuezooText.kt`.

---

## Shapes

```
PillShape          RoundedCornerShape(50)   text buttons, price button
SquircleSmall      exponent 3.5f            chips, badges
SquircleMedium     exponent 4.0f            icon buttons, tiles
SquircleLarge      exponent 5.0f            result card (40dp equivalent)
CardShape          RoundedCornerShape(20dp) game cards
```

**Shape usage:**
- `PillShape` → HuezooButton, PriceButton
- `SquircleMedium` → HuezooIconButton (X, ✓, info, back — 48×48dp)
- `SquircleLarge` → ResultCard outer frame
- `CardShape` → GameCard
- `SquircleSmall` → Badges, currency pill, small tags

---

## Components

### HuezooButton
Candy-style pill button. Two-layer Box: shelf fixed at `offset(x=0, y=shelfHeight)`, face translates Y on press. Spring release. No X offset — bottom-only shelf is what makes it feel like a game button, not a brutalist one.

Variants: Primary (Cyan), Confirm (Green), Danger (Magenta), Score (Yellow), Try (Blue), Ghost (transparent).
Text: always `HuezooLabelLarge`, color computed via `.onColor`.

### HuezooIconButton
Square squircle (48×48dp) with 4dp bottom shelf. Same press animation. Four variants: Dismiss (red), Confirm (green), Back (dark), Info (cyan). **Use instead of any TopAppBar back arrow in game screens.**

### PriceButton
Wide pill (56dp tall, full width) with 6dp bottom shelf. Bright green face. Price text as `HuezooHeadlineMedium`, ExtraBold. Used only for purchase CTAs.

### GameCard
Layered candy card. Outer frame: identity color fill, CardShape. Inner panel: SurfaceL2, inset 5dp. Illustration area: 90dp, identity color at 12% opacity. Badge overlaid top-right of illustration area. 8dp bottom shelf in darkened identity color. Press translates Y only — no X.

### ResultCard
Share-ready end-of-game card. Inner panel on Background color. 8dp bottom shelf. Spring entrance (slide up 60dp + scale 0.9→1.0 + fade). Count-up animation for score and ΔE values.

### DeltaEBadge
Shows ΔE value after each round. Color reflects difficulty: Cyan (easy, >3.0), Yellow (medium, 1.5–3.0), Magenta (hard, <1.5). Antonio Bold display size for the number.

### RoundIndicator
Row of dots for game progress. Active dot: animated scale to 1.2f + white border. Inactive: smaller, SurfaceL3. Completed: accent green fill.

### CurrencyPill
Display-only pill showing gem icon + amount. SurfaceL2 background, 1.5dp GemGreen border. No shelf, no press animation.

---

## Game Identity Colors

Each game has an identity color used as: card frame, card background tint, ΔE badge color, result card glow.

| Game | Identity Color | Hex |
|---|---|---|
| The Threshold | Electric Purple | `#9B5DE5` |
| Daily Challenge | Electric Yellow | `#FFE600` |
| What's My Delta E? | Cyan | `#00E5FF` |
| Odd Swatch Out | Electric Green | `#00F5A0` |
| Mix to Match | Hot Magenta | `#FF2D78` |
| Color Memory | Electric Orange | `#FF9500` |

---

## What NOT to Do

```
❌ NEVER use TopAppBar with back arrow in game screens
✅ Use HuezooIconButton(variant = Back / Dismiss) instead

❌ NEVER use diagonal shadow (x+y offset)
✅ Use bottom shelf only (x=0, y=shelfHeight)

❌ NEVER use emoji as UI elements
✅ Use Res.drawable.* vector composables

❌ NEVER use bare Text() composable
✅ Use the typed HuezooText variants from HuezooText.kt

❌ NEVER use MaterialTheme.colorScheme.onBackground on card/button surfaces
✅ Use HuezooColors.TextPrimary / TextSecondary / TextDisabled explicitly

❌ NEVER hardcode Color.White or Color.Black for text on colored backgrounds
✅ Use .onColor from ColorExt.kt

❌ NEVER use light theme — Huezoo is dark only
```

---

## File Reference

```
composeApp/src/commonMain/
├── composeResources/
│   ├── drawable/          ← all vectors (ic_close, ic_check, game art, etc.)
│   └── font/              ← Antonio, Fredoka, Space Grotesk TTF files
└── kotlin/xyz/ksharma/huezoo/
    ├── ui/theme/
    │   ├── Color.kt         ← all color tokens + darken() extension
    │   ├── ColorExt.kt      ← contrastRatio(), foregroundColor(), onColor
    │   ├── Shape.kt         ← SquircleShape presets + PillShape
    │   ├── Typography.kt    ← three-font system wired into huezooTypography()
    │   └── Theme.kt         ← HuezooTheme (dark-only)
    └── ui/components/
        ├── HuezooText.kt        ← ALL typed text composables — only file using Text()
        ├── HuezooButton.kt      ← pill + shelf + spring press
        ├── HuezooIconButton.kt  ← squircle icon buttons (X, ✓, back, info)
        ├── PriceButton.kt       ← purchase CTA
        ├── GameCard.kt          ← frame + inner panel + shelf
        ├── ResultCard.kt        ← end-of-game share card
        ├── SwatchBlock.kt       ← color swatch with states
        ├── DeltaEBadge.kt       ← animated ΔE display
        ├── RoundIndicator.kt    ← round progress dots
        ├── CurrencyPill.kt      ← gem + amount display
        └── HuezooBottomSheet.kt ← paywall / name entry sheet
```
