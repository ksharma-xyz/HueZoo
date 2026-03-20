# Design System Strategy: Kinetic High-Fidelity Gaming

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Kinetic Powerhouse."** 

This is not a flat, static interface; it is a living, breathing mechanical entity. Inspired by the high-stakes, hyper-saturated world of mobile competitive gaming, the system rejects traditional "clean" minimalism in favor of **Maximalist Intentionality**. We achieve a premium feel not through simplification, but through depth, texture, and physical weight. 

The layout breaks the standard grid by utilizing "forced perspective" and layered depth. Elements shouldn't just sit next to each other; they should overlap, "pop" out of containers, and exist within a 3D environment characterized by aggressive lighting and tactile surfaces.

---

## 2. Colors
Our palette is rooted in a deep, nocturnal base to allow neon accents to achieve maximum luminosity.

- **Background Strategy:** The `background` (#0d0d16) is never a flat fill. It must be textured with radial glows using `surface_variant`, overlaid with 15% opacity diagonal scanlines, and subtle particle effects using `primary` at 5% opacity.
- **Accents:** 
    - `primary` (#81ecff / Neon Cyan): High-energy actions and progression.
    - `secondary` (#bb83ff / Electric Purple): Rare items and secondary highlights.
    - `tertiary` (#ff6c92 / Hot Magenta): Critical alerts and ultra-premium CTAs.
- **The "No-Line" Rule:** Prohibit the use of 1px solid lines for sectioning. Boundaries are created through background shifts. For example, a `surface_container_high` module should sit on a `surface` background. The transition defines the edge.
- **The "Glass & Gradient" Rule:** Use `surface_bright` with 40% opacity and a 20px backdrop-blur for floating overlays. Main action buttons must use a linear gradient from `primary_container` to `primary_dim` to provide the "soul" of a physical, backlit plastic button.

---

## 3. Typography
We utilize a three-font stack to differentiate between data, personality, and utility.

- **Display & Headlines (Plus Jakarta Sans):** Used for large-scale marketing beats and section headers. These should be set with tight tracking and a "comic-style" black outline (2px) to ensure they pop against busy backgrounds.
- **Titles & Personality (Fredoka):** Used for celebratory text and item names. This rounded, friendly font balances the "extreme" nature of the UI with accessibility.
- **Numbers (Antonio Bold):** High-impact, condensed font for scores, currency, and countdowns. It conveys speed and urgency.
- **UI & Body (Space Grotesk):** Our workhorse. Used for descriptions and functional labels. Its geometric nature maintains the "tech-forward" gaming aesthetic.

---

## 4. Elevation & Depth
Depth is not an effect; it is a structural requirement.

- **The Layering Principle:** Use the `surface_container` tiers to stack importance. 
    - `surface_container_lowest`: Background wells and "recessed" slots.
    - `surface_container_highest`: Active cards and interactive objects.
- **3D Shelf Shadows:** Instead of centered blurs, use "shelf" shadows—offset Y-axis shadows (e.g., 8px) with 15% opacity `on_secondary` to give cards a physical weight that feels like they are hovering over a surface.
- **The "Ghost Border" Fallback:** If a container needs an edge for legibility, use `outline_variant` at 20% opacity. For primary items, use a "Hard Highlight"—a 2px inner-bevel on the top-left edge using `primary_fixed` at 40% opacity to simulate overhead lighting.
- **Glassmorphism:** Use for temporary states (modals/tooltips). The background blur creates a "frosted polycarbonate" effect that feels expensive and integrated.

---

## 5. Components

### Buttons (Tactile Triggers)
- **Primary:** Gradient `primary` to `primary_dim`. 4px bottom "lip" (darker shade) to simulate a 3D physical button. On press, the element shifts 2px down.
- **Secondary:** `surface_container_highest` with a `primary` "Ghost Border."
- **Typography:** Antonio Bold, Uppercase.

### Cards & Slots (The "Brawl" Frame)
- **Rules:** Forbid divider lines. Use `surface_container_low` for the card body and `surface_container_lowest` for internal data slots (nests). 
- **Edges:** `md` (0.75rem) or `lg` (1rem) roundedness. Cards must feature a "thick" 3D border effect using a 3px stroke of `outline_variant`.

### Progression Bars (Neon Tracks)
- **Track:** `surface_container_lowest`.
- **Fill:** Gradient of `primary` to `secondary`.
- **Glow:** Add a 10px outer glow using the `primary` color at 30% opacity to simulate a glowing LED tube.

### Chips & Tags
- **Style:** Pill-shaped (`full` roundedness). Use `secondary_container` for the background with `on_secondary_container` text.
- **Visual:** Include a small 12px animated pictogram/icon to the left of the label.

---

## 6. Do's and Don'ts

### Do
- **Do** overlap elements. Let character art or icons break the boundaries of their parent containers.
- **Do** use "Comic Outlines." Apply a 2px-4px dark stroke around high-contrast icons to separate them from textured backgrounds.
- **Do** use saturated glows for "Max" states or "Unlocked" items.

### Don't
- **Don't** use pure greys. Every neutral should be tinted with the `background` hue (#0d0d16) to maintain atmospheric consistency.
- **Don't** use 1px thin, flat lines. If a separator is needed, use a 4px gap of vertical whitespace or a tonal shift.
- **Don't** let the background remain static. Subtle movement (drifting particles or shifting glows) is essential for the "game-grade" feel.
- **Don't** use standard "drop shadows." Use directional "shelf" shadows that imply a consistent light source from the top-center of the screen.