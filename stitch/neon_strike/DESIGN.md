# Design System Strategy: High-Intensity Kineticism

## 1. Overview & Creative North Star
**Creative North Star: "The Neon Fortress"**

This design system rejects the ephemeral, flat nature of modern web design in favor of something structural, tactile, and high-stakes. It is built on the philosophy of **Kinetic Weight**—the idea that every UI element has physical mass and light energy. 

We break the "standard template" look by utilizing "chunky" oversized components, intentional asymmetrical layering, and high-intensity, zero-blur light signatures. Unlike typical interfaces that rely on soft shadows to imply depth, this system uses sharp geometry and "rim lighting" to carve objects out of the deep space background. The result is a high-fidelity mobile gaming experience that feels like a physical console within the user's hands.

---

## 2. Colors: High-Voltage Contrast
The palette is rooted in a deep-space abyss, allowing the neon accents to serve as functional light sources rather than mere decoration.

*   **Background (`#0d0d16`):** The absolute foundation. All UI elements emerge from this void.
*   **Primary Cyan (`#81ecff`):** Used for "Action Energy." This is your go-to for confirmation, progression, and active states.
*   **Secondary Magenta (`#ff6c92`):** Used for "Intensity." High-tier rewards, premium triggers, and alerts.
*   **Tertiary Yellow (`#fff2a8`):** Used for "Critical Focus." Scoreboards, currency, and "New" indicators.

**The "No-Line" Rule**
Borders are strictly prohibited for the purpose of sectioning content. Instead, use **Surface Nesting**. A card (surface-container-high) should sit inside a section (surface-container-low) to create a natural transition. If a boundary feels too soft, use a "rim light" (an inner stroke of 1-2px at 20% opacity of the accent color) rather than a solid grey line.

**The Glass & Gradient Rule**
To achieve "High-Fidelity" depth, CTAs must utilize a vertical gradient from the `primary` to `primary_container`. For floating overlays, use a semi-transparent `surface_variant` with a heavy backdrop-blur to allow the background neon glows to bleed through the glass.

---

## 3. Typography: The Scoreboard Hierarchy
The typography is designed to be loud, clear, and unyielding.

*   **Display & Headlines (Plus Jakarta Sans):** Our "Brand Voice." Large, oversized, and authoritative. Used to anchor screens and announce major milestones.
*   **Titles (Fredoka):** The "Friendly Commander." This rounded, bold face softens the technical intensity of the neon, making headers feel accessible and playful.
*   **Numbers (Antonio Bold):** The "Scorekeeper." This condensed, high-impact font is used exclusively for stats, levels, and timers. It mimics high-end stadium scoreboards.
*   **Labels & Body (Space Grotesk):** The "Tactical Data." Its monospaced feel provides technical precision for descriptions and labels.

---

## 4. Elevation & Depth: The Layering Principle
We do not use blurry shadows. Depth is communicated through **Tonal Layering** and **Light Stacking**.

*   **The Bottom Shelf:** Cards must have a 3D "bottom shelf" or chamfered edge. This is achieved by adding a 4-8px solid offset on the Y-axis using a darker version of the card's color (e.g., `primary_dim` under a `primary` surface).
*   **Zero-Blur Glows:** To create a "Neon Strike" effect, use multiple layers of box-shadows with **0 blur**. 
    *   *Example:* `box-shadow: 0 0 0 2px #00E5FF, 0 0 0 4px rgba(0, 229, 255, 0.3);`
*   **Rim Lighting:** Every panel should have a 1px inner-border (inset shadow) on the top and left edges using a lighter tint (`on_surface_variant` at 30%) to simulate a metal edge catching a light source.
*   **Physical Sinking:** On tap, components do not just change color; they physically move. Use a `translateY(4px)` transform and remove the "bottom shelf" offset to simulate the button being pressed into the device.

---

## 5. Components: Chunky & Tactile

### Buttons
*   **Primary**: Oversized padding (`1.7rem` height). Gradient fill (`primary` to `primary_dim`). Solid 4px bottom shelf. Text in `plusJakartaSans` (Headline-SM).
*   **Secondary**: `secondary` border-only with a 2px inner rim light. No fill, but uses a subtle `surface_container_high` background.

### Cards & Panels
*   **The "Pro Pass" Container**: Forbid divider lines. Separate content using `surface_container_highest` blocks nested within `surface_dim` backgrounds. Use a `1rem` (lg) corner radius to maintain the "chunky" aesthetic.
*   **Rim-Lit Borders**: Panels should look like chamfered metal. Use `outline_variant` at 20% for the main stroke, with a high-intensity `primary` glow on the corners only.

### Progress Tracks
*   Instead of a thin line, use a thick `0.7rem` bar. The "filled" portion should have a 0-blur glow that spills over the track edges, making the progress feel like it’s "charging" the UI.

### Input Fields
*   **Active State**: The entire border of the input field should ignite with a `primary` glow. The label (Space Grotesk) should "pop" upward and increase in weight.

---

## 6. Do's and Don'ts

### Do:
*   **DO** use oversized icons and text. If it feels too big, it’s probably the right size for this system.
*   **DO** stack multiple 0-blur shadows to create "hard" light rings around important assets (like gems or tokens).
*   **DO** ensure that every interactive element has a "pressed" state that involves physical movement.

### Don't:
*   **DON'T** use 1px solid grey borders. They break the high-fidelity game immersion.
*   **DON'T** use soft, fuzzy shadows (blur > 2px). If you need depth, use color shifts or solid offsets.
*   **DON'T** use standard "web" spacing. Follow the spacing scale (`3.5rem` and above) to give elements enough room to breathe despite their chunky size.
*   **DON'T** mix the glow colors haphazardly. Assign one accent color per "tier" of information (Cyan for common, Magenta for rare, Yellow for legendary).