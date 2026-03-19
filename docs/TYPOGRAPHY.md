# Huezoo — Typography Reference

> **For AI assistants:** This document is the authoritative source on how text is rendered in the Huezoo app. When writing or reviewing any Composable that contains text, consult this file. All rules here are strict — no exceptions without updating this doc first.

---

## Core Rule

**Never use `androidx.compose.material3.Text` directly in any screen or component.**

Use the typed `HuezooText` composables from:
```
xyz.ksharma.huezoo.ui.components.HuezooText.kt
```

The only place `Text()` is permitted is inside `HuezooText.kt` itself (the internal implementation layer).

---

## Font Families

Three fonts. Each has a strict semantic role. Do not mix them up.

### Antonio — Numbers and Power

```kotlin
// Loaded in Typography.kt
Font(Res.font.antonio_regular, FontWeight.Normal)
Font(Res.font.antonio_medium,  FontWeight.Medium)
Font(Res.font.antonio_bold,    FontWeight.Bold)
```

**Use for:** ΔE scores, game stats (SCORE, ROUNDS), hero numbers, the app name, currency amounts, any numeric readout that carries game weight.

**Feel:** Bold, condensed, confident. Numbers feel heavy and earned. Like a scoreboard font.

**Never use for:** Descriptive text, button labels, subtitles, anything that isn't a number or a power headline.

---

### Fredoka — Warmth and Play

```kotlin
Font(Res.font.fredoka_regular,  FontWeight.Normal)
Font(Res.font.fredoka_semibold, FontWeight.SemiBold)
Font(Res.font.fredoka_bold,     FontWeight.Bold)
```

**Use for:** GameCard titles, bottom sheet headings, paywall section titles, any text that names a thing (game name, feature name, section label at medium hierarchy).

**Feel:** Round, approachable, fun without being childish. Makes the app feel like a game, not a dashboard.

**Never use for:** Long body paragraphs, button labels, small metadata, numeric readouts.

---

### Space Grotesk — Clarity and Structure

```kotlin
Font(Res.font.space_grotesk_regular,  FontWeight.Normal)
Font(Res.font.space_grotesk_medium,   FontWeight.Medium)
Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold)
Font(Res.font.space_grotesk_bold,     FontWeight.Bold)
```

**Use for:** Body descriptions, button labels, metadata, badge text, tries counts, personal best labels, captions, all small UI labels.

**Feel:** Precise, readable at small sizes, geometric. Keeps the UI information-dense without looking cluttered.

**Never use for:** Hero numbers, card titles, the app name, anything that needs warmth or weight.

---

## Typography Scale — Full Reference

All slots are defined in `Typography.kt` inside `huezooTypography()`.

| Slot | Composable | Font | Weight | Size | Line Height | Default Use |
|---|---|---|---|---|---|---|
| `displayLarge` | `HuezooDisplayLarge` | Antonio | Bold | 56sp | 60sp | ΔE hero number on ResultCard, big score reveals |
| `displayMedium` | `HuezooDisplayMedium` | Antonio | Bold | 40sp | 44sp | SCORE / ROUNDS stats on ResultCard |
| `displaySmall` | `HuezooDisplaySmall` | Antonio | Bold | 28sp | 32sp | DeltaEBadge number, compact numeric readouts |
| `headlineLarge` | `HuezooHeadlineLarge` | Antonio | Bold | 40sp | 44sp | App name "Hue Zoo", screen hero title |
| `headlineMedium` | `HuezooHeadlineMedium` | Fredoka | SemiBold | 28sp | 32sp | Section headings, dialog titles, paywall title |
| `headlineSmall` | `HuezooHeadlineSmall` | Antonio | Medium | 20sp | 24sp | Currency pill amount, compact inline numbers |
| `titleLarge` | `HuezooTitleLarge` | Fredoka | Bold | 22sp | 26sp | Card headers, sheet titles |
| `titleMedium` | `HuezooTitleMedium` | Fredoka | SemiBold | 20sp | 24sp | GameCard title, bottom sheet heading |
| `titleSmall` | `HuezooTitleSmall` | Fredoka | Regular | 16sp | 20sp | Secondary titles, list item sub-headers |
| `bodyLarge` | `HuezooBodyLarge` | Space Grotesk | Regular | 18sp | 26sp | Long-form description text |
| `bodyMedium` | `HuezooBodyMedium` | Space Grotesk | Regular | 16sp | 22sp | GameCard subtitle, game description |
| `bodySmall` | `HuezooBodySmall` | Space Grotesk | Regular | 14sp | 20sp | Captions, helper text, hints |
| `labelLarge` | `HuezooLabelLarge` | Space Grotesk | Bold | 16sp | 20sp | Button labels, prominent chips |
| `labelMedium` | `HuezooLabelMedium` | Space Grotesk | Medium | 13sp | 16sp | Secondary labels, list metadata |
| `labelSmall` | `HuezooLabelSmall` | Space Grotesk | Medium | 12sp | 14sp | Badges, tries text, personal best, stat headers |

---

## HuezooText Composables

Every composable in this system shares the same signature pattern:

```kotlin
@Composable
fun HuezooXxx(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = <default — see below>,
    fontWeight: FontWeight? = null,          // null = use weight from TextStyle
    textAlign: TextAlign? = null,
    maxLines: Int = <default per variant>,
    overflow: TextOverflow = <default per variant>,
)
```

### Color Defaults by Variant

Colors come from `MaterialTheme.colorScheme` — they automatically adapt to dark or light theme.

| Variants | Default Color | M3 Role | Dark theme value | Light theme value |
|---|---|---|---|---|
| Display, Headline, Title | `onBackground` | Primary text | `#FFFFFF` (white) | `#0D0D1A` (near-black) |
| Body, LabelSmall, LabelMedium | `onSurfaceVariant` | Secondary text | `#8888AA` (muted purple-grey) | `#55556A` |
| LabelLarge | `onBackground` | Buttons, prominent labels | `#FFFFFF` | `#0D0D1A` |

When an **accent or dynamic color** is needed (e.g. badge color, identity color), pass it explicitly via the `color` parameter:

```kotlin
// Correct — explicit accent color for a badge
HuezooDisplaySmall(text = "4.2", color = HuezooColors.AccentCyan)

// Correct — dynamic color driven by game state
HuezooLabelSmall(text = triesText, color = identityColor)

// Correct — override to a disabled/muted token
HuezooLabelSmall(text = personalBest, color = HuezooColors.TextDisabled)

// Wrong — do not compute the color from isSystemInDarkTheme() manually
// The default already handles dark/light. Only pass color when it's truly custom.
```

### fontWeight Override Rule

The `fontWeight` parameter overrides the weight baked into the `TextStyle`. Use it sparingly — only when a specific context genuinely needs a different weight for the same semantic slot:

```kotlin
// OK — badge text needs ExtraBold emphasis
HuezooLabelSmall(text = "DAILY", fontWeight = FontWeight.ExtraBold, color = identityColor)

// OK — button label ExtraBold for tactile feel
HuezooLabelLarge(text = "Play", color = resolvedContent, fontWeight = FontWeight.ExtraBold)

// Bad — don't apply fontWeight just to "make it bolder without thinking"
// Trust the default weight first; only override with clear reason
```

---

## Usage by Component

This section maps each component to the correct composables. Use as a checklist when reviewing or writing components.

### GameCard

```kotlin
HuezooTitleMedium(text = title)                              // Fredoka SemiBold 20sp
HuezooBodyMedium(text = subtitle)                            // Space Grotesk 16sp
HuezooLabelSmall(text = badgeText, color = HuezooColors.Background,
    fontWeight = FontWeight.ExtraBold)                       // badge on illustration
HuezooLabelSmall(text = triesText, color = identityColor,
    fontWeight = FontWeight.SemiBold)                        // tries remaining
HuezooLabelSmall(text = personalBest, color = HuezooColors.TextDisabled)  // PB stat
```

### ResultCard

```kotlin
HuezooLabelSmall(text = gameTitle.uppercase(), color = identityColor,
    fontWeight = FontWeight.ExtraBold)                       // game name tag
HuezooLabelSmall(text = "COLOR DELTA")                      // stat header
HuezooDisplayLarge(text = formattedDeltaE)                   // Antonio 56sp hero ΔE
HuezooLabelSmall(text = "SCORE" / "ROUNDS")                  // stat column header
HuezooDisplayMedium(text = score / rounds)                   // Antonio 40sp stat value
HuezooLabelMedium(text = percentileText)                     // "Better than X%..."
```

### DeltaEBadge

```kotlin
HuezooDisplaySmall(text = formatted, color = badgeColor)     // Antonio Bold 28sp
HuezooLabelSmall(text = label, color = badgeColor.copy(0.8f)) // "ΔE" label
```

### CurrencyPill

```kotlin
HuezooHeadlineSmall(text = amount.toString(), color = HuezooColors.TextPrimary) // Antonio Medium 20sp
```

### HuezooButton

```kotlin
HuezooLabelLarge(text = text, color = resolvedContent,
    fontWeight = FontWeight.ExtraBold)                       // Space Grotesk Bold 16sp
```

### PriceButton

```kotlin
HuezooHeadlineMedium(text = price, color = textColor,
    fontWeight = FontWeight.ExtraBold)                       // Fredoka SemiBold 28sp
```

### HuezooBottomSheet (preview / content)

```kotlin
HuezooTitleMedium(text = "Unlock Forever — \$2")             // Fredoka SemiBold 20sp
```

---

## Contrast & Color Safety

### The problem this solves

In a game app, text often sits directly on top of accent colors — button faces, badges, identity-color chips. Hardcoding `Color.White` or `Color.Black` fails silently: white text on bright cyan is unreadable (contrast ratio ~1.5:1); white text on green (#22C55E) is ~2.25:1 — both fail WCAG AA which requires 4.5:1.

The `onColor` extension in `ColorExt.kt` solves this automatically.

### Available utilities (in `ui/theme/ColorExt.kt`)

```kotlin
// Contrast ratio between two colors (WCAG 2.1 formula)
val ratio: Float = HuezooColors.AccentCyan.contrastRatio(HuezooColors.Background) // ~12.9

// Full foregroundColor with optional preferred color
val textColor: Color = HuezooColors.AccentCyan.foregroundColor()           // → #0D0D1A (dark)
val textColor: Color = HuezooColors.Background.foregroundColor()           // → #FFFFFF (white)
val textColor: Color = someColor.foregroundColor(preferred = accentColor)  // tries accent first

// Shorthand property
val textColor: Color = faceColor.onColor  // same as foregroundColor() with no preference
```

### Constants

| Constant | Value | Meaning |
|---|---|---|
| `CONTRAST_AA_NORMAL` | 4.5f | WCAG AA for normal text (< 18sp regular, < 14sp bold) |
| `CONTRAST_AA_LARGE` | 3.0f | WCAG AA for large text (≥ 18sp regular or ≥ 14sp bold) |

### Resolution order for `foregroundColor()`

1. `preferred` — if provided AND its contrast ratio with the background ≥ 4.5, use it
2. `#0D0D1A` (near-black) — preferred for bright backgrounds
3. `#FFFFFF` (white) — preferred for dark backgrounds
4. `Color.Black` / `Color.White` — last-resort fallbacks if none of the above pass

### Contrast values for Huezoo accent colors (reference)

| Color | Hex | Luminance | vs #0D0D1A (dark) | vs #FFFFFF (white) | Use dark text? |
|---|---|---|---|---|---|
| AccentCyan | `#00E5FF` | 0.638 | ~13:1 ✅ | ~1.5:1 ❌ | **Yes** |
| AccentYellow | `#FFE600` | 0.929 | ~19:1 ✅ | ~1.1:1 ❌ | **Yes** |
| AccentGreen | `#00F5A0` | 0.633 | ~12.8:1 ✅ | ~1.5:1 ❌ | **Yes** |
| ActionConfirm | `#22C55E` | 0.416 | ~8.8:1 ✅ | ~2.3:1 ❌ | **Yes** |
| AccentMagenta | `#FF2D78` | 0.246 | ~5.6:1 ✅ | ~3.6:1 ❌ | **Yes** |
| ActionTry | `#3B82F6` | 0.234 | ~5.4:1 ✅ | ~3.7:1 ❌ | **Yes** |
| ActionDismiss | `#EF4444` | 0.213 | ~5.0:1 ✅ | ~3.9:1 ❌ | **Yes** |
| AccentPurple | `#9B5DE5` | 0.148 | ~3.7:1 ❌ | ~5.7:1 ✅ | **No** (use white) |
| Background | `#080810` | 0.003 | ~1.1:1 ❌ | ~19:1 ✅ | **No** (use white) |
| SurfaceL2 | `#1C1C2E` | 0.012 | ~1.4:1 ❌ | ~14:1 ✅ | **No** (use white) |

> Note: AccentPurple is the only accent that correctly gets white text from `onColor` — all others get near-black.

### Rules

**Always use `.onColor` when text sits directly on a colored background:**

```kotlin
// ✅ Correct — any button, badge, or chip with a colored face
HuezooLabelLarge(text = "Play", color = faceColor.onColor)
HuezooLabelSmall(text = "DAILY", color = identityColor.onColor)

// ✅ Correct — explicit preferred color checked for contrast first
val label = HuezooColors.AccentCyan.foregroundColor(preferred = Color.White) // → #0D0D1A (white fails)
val label = HuezooColors.Background.foregroundColor(preferred = Color.White) // → #FFFFFF (white passes)

// ❌ Never hardcode text color for colored backgrounds
HuezooLabelLarge(text = "Play", color = Color.White)       // may fail on bright colors
HuezooLabelLarge(text = "Play", color = Color.Black)       // may fail on dark colors
HuezooLabelLarge(text = "Play", color = HuezooColors.TextPrimary) // wrong — TextPrimary is for page text

// ❌ Never use isSystemInDarkTheme() to decide text color on a component background
val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black  // brittle, mode-dependent
```

**Exception — Ghost / transparent buttons:** When a button has `Color.Transparent` background, there is no real background to compute contrast against. Use the accent color of the border as the text color:

```kotlin
HuezooButtonVariant.Ghost -> ButtonColors(
    bg = Color.Transparent,
    content = HuezooColors.AccentCyan, // fixed — we know the surface behind is always dark
    border = HuezooColors.AccentCyan,
)
```

---

## Dark / Light Mode

`HuezooTheme` switches between `huezooDarkColorScheme` and `huezooLightColorScheme` based on `isSystemInDarkTheme()`.

The color defaults in `HuezooText` composables reference `MaterialTheme.colorScheme.onBackground` and `MaterialTheme.colorScheme.onSurfaceVariant` — so they automatically use the correct value for the current theme. You never need to check `isSystemInDarkTheme()` inside a component to set text color.

Only `HuezooColors.*` tokens (like `AccentCyan`, `TextDisabled`, `ShelfCyan`) are currently static. When light mode support for accent colors is added, those tokens will also migrate to `MaterialTheme.colorScheme` roles. Until then, static accent colors are acceptable since the game is primarily dark.

### Color scheme mappings

| M3 Role | Dark value | Light value | Used for |
|---|---|---|---|
| `onBackground` | `#FFFFFF` | `#0D0D1A` | Primary text (titles, display, labels) |
| `onSurface` | `#FFFFFF` | `#0D0D1A` | Text on card surfaces |
| `onSurfaceVariant` | `#8888AA` | `#55556A` | Secondary text (body, metadata) |
| `background` | `#080810` | `#F6F6FF` | App background |
| `surface` | `#12121E` | `#FFFFFF` | Card surface |
| `surfaceVariant` | `#1C1C2E` | `#EDECF8` | Elevated elements, sheets |
| `primary` | `#00E5FF` | `#006E8F` | Primary interactive, CTAs |

---

## How to Add a New Text Style

1. **Decide if an existing slot fits.** Re-using existing slots is strongly preferred over adding new ones.
2. If a new slot is genuinely needed, add a `TextStyle` entry to `huezooTypography()` in `Typography.kt`.
3. Add a corresponding `@Composable fun HuezooXxx(...)` to `HuezooText.kt` following the standard signature.
4. Document the new entry in the table above in this file.
5. **Do not** add a custom font family outside the three (Antonio / Fredoka / Space Grotesk) without updating this doc and `Typography.kt`.

---

## What Never to Do

```kotlin
// ❌ Bare Text — no typography, no color defaults
Text("Hello", color = Color.White)

// ❌ Inline style — bypasses the typed system
Text("Hello", style = MaterialTheme.typography.titleMedium, color = HuezooColors.TextPrimary)

// ❌ isSystemInDarkTheme() for text color — use MaterialTheme.colorScheme instead
val color = if (isSystemInDarkTheme()) Color.White else Color.Black
Text("Hello", color = color)

// ❌ Hardcoded font family — bypasses Typography.kt
Text("Hello", fontFamily = FontFamily(Font(Res.font.antonio_bold)))

// ❌ fontWeight on display/title styles to just "make it look bigger"
HuezooDisplayLarge(text = score, fontWeight = FontWeight.ExtraBold) // already Bold, don't override

// ✅ Correct — typed composable, explicit accent color override
HuezooDisplaySmall(text = formatted, color = badgeColor)

// ✅ Correct — defaults handle color automatically
HuezooTitleMedium(text = title)

// ✅ Correct — fontWeight override only when semantically needed
HuezooLabelLarge(text = "Play", color = resolvedContent, fontWeight = FontWeight.ExtraBold)
```

---

## File Locations

| File | Purpose |
|---|---|
| `ui/theme/Typography.kt` | Font families, `huezooTypography()`, all `TextStyle` definitions |
| `ui/theme/Theme.kt` | `HuezooTheme`, dark/light color scheme switching |
| `ui/theme/Color.kt` | `HuezooColors` static tokens (accent, shelf, surface, game identity) |
| `ui/theme/ColorExt.kt` | `contrastRatio`, `foregroundColor`, `onColor` — contrast utilities |
| `ui/components/HuezooText.kt` | All typed `HuezooText` composables — the only file that imports `Text` |
| `composeResources/font/` | `.ttf` files: `antonio_*`, `fredoka_*`, `space_grotesk_*` |
