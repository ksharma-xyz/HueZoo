```markdown
# Design System: Premium Arcade Kineticism

## 1. Overview & Creative North Star
The Creative North Star for this system is **"The Kinetic Vault."** This is not a standard mobile game UI; it is a high-end, tactile instrument where light behaves as a physical material. We are moving away from the "flat" web and into a space of **Premium Arcade Kineticism**. 

The design rejects generic rounded corners (all radii are set to `0px`) in favor of sharp, aggressive architectural precision. We break the "template" look through **intentional asymmetry**—offsetting UI elements to create a sense of forward motion—and by using high-intensity neon "rim lighting" to define edges rather than traditional borders.

## 2. Colors & Light Physics
The palette is built on a foundation of "Deep Background" darkness, allowing our neon primaries to function as light sources that illuminate the UI.

### The Palette
- **Deep Background (`surface`):** `#13131b` — The void. Everything emerges from here.
- **Electric Cyan (`primary_container`):** `#00e5ff` — Level 1 / Actionable primary light.
- **Neon Magenta (`secondary_container`):** `#e00363` — Level 2 / High-energy secondary.
- **Solar Gold (`tertiary_container`):** `#e9d200` — Level 3 / Achievement and peak intensity.

### The "No-Line" Rule
**Prohibit 1px solid borders.** Boundaries are defined by light and depth, not strokes. Use `surface-container-low` against `surface` to define regions. If a boundary must be emphasized, use a **Neon Rim Light**: a 1px inner-shadow or glow using the `primary` or `secondary` tokens at 100% opacity, but only on one or two edges (top/left) to simulate a physical light source.

### Surface Hierarchy & Nesting
Treat the UI as a series of **3D physical shelves**. 
- **Base:** `surface` (#13131b)
- **Recessed Areas:** `surface_container_lowest` (#0d0d16) for background tracks or "wells."
- **Raised Shelves:** `surface_container_high` (#292932) for interactive modules.
- **The "Glass & Gradient" Rule:** Floating overlays must use a `surface` tint with a 20px backdrop-blur. Apply a linear gradient from `primary` to `primary_fixed_variant` at 10% opacity to give the "glass" a colored edge-tint.

## 3. Typography
We utilize a high-contrast pairing to balance "Arcade" energy with "Premium" legibility.

- **Display & Headline (Antonio Bold):** Used for game scores, level numbers, and the Wordmark. This is our "Machined" typeface. It should feel industrial and tall. Use `display-lg` for hero numbers to dominate the screen.
- **UI Labels & Body (Space Grotesk):** Our "Technical" typeface. Space Grotesk’s monospaced-adjacent personality provides the precision needed for a color-difference game. 

**Typography Logic:**
- **Numbers:** Always Antonio Bold. Level counters use `primary` color tokens.
- **Labels:** Always Space Grotesk. Use `label-md` in all-caps with `0.1rem` letter spacing for a "NASA-spec" aesthetic.

## 4. Elevation & Depth: Tonal Layering
Traditional shadows are too "soft" for this system. We replace them with **Hard-Edge Light Stacking.**

- **The Layering Principle:** To lift a button, do not use a drop shadow. Instead, place a `surface_container_highest` (#34343d) shape 4px below the main component, offset to the bottom-right, to create a "Physical Shelf."
- **Sharp Glows:** For active states, use a `0px` blur, `2px` spread outer glow. It must look like a neon tube, not a soft lamp. Use the `primary` token for Level 1 elements.
- **The "Ghost Border" Fallback:** If a container needs separation on a dark background, use `outline_variant` at 15% opacity. This "Ghost Border" should be almost invisible, felt rather than seen.

## 5. Components

### The Wordmark
- **Rule:** "Hue" is rendered in solid `on_surface`. "Zoo" is rendered as an outline (0.5px stroke) using the `primary` token. This creates a "Solid vs. Light" duality.

### Buttons (The "3D Shelf" Variant)
- **Primary:** Background is `primary_container`. Text is `on_primary_fixed`. No rounded corners. The button sits on a "shelf" of `primary_fixed_variant`.
- **States:** On hover/press, the button shifts -2px vertically (closing the gap to the shelf) and triggers a sharp, non-blurry glow (`primary`).

### Nodes (The Game Grid)
- **Visuals:** Sharp square tiles. No dividers. Use `spacing-1` (0.2rem) as the gutter width.
- **Transitions:** When a node appears or changes, use an **Unfolding Circular Animation**. The new color should "wipe" from the center in a perfect circle, even though the container is a sharp square.

### Input Fields & Controls
- **Style:** Underlined only. Use `outline` token for the default state. Upon focus, the underline transforms into a `primary` neon beam with a 2px sharp glow.
- **Checkboxes:** Square only. When checked, the box fills with `secondary_container` and emits a magenta "rim light" onto the surrounding surface.

### Cards & Lists
- **Rule:** Forbid divider lines. Separate list items using `surface_container_low` for even items and `surface_container_lowest` for odd items, creating a "zebra-striping" of depth rather than lines.

## 6. Do's and Don'ts

- **DO:** Use intentional asymmetry. Align labels to the far right while headers stay far left to create tension.
- **DO:** Use the Spacing Scale strictly. `spacing-20` (4.5rem) should be used for major section breathing room to maintain the "Premium" feel.
- **DON'T:** Use blur values over 5px. This system is about "Premium Arcade" precision; soft blurs feel "cheap" and "web-standard."
- **DON'T:** Use 100% white. Use `on_surface` (#e4e1ed) for text to keep the neon colors as the highest-luminance elements on the screen.
- **DON'T:** Round any corners. The `0px` rule is absolute. To create "softness," use color gradients, not corner radii.