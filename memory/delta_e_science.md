---
name: delta_e_science
description: Scientific explanation of Delta E for use in app copy, help sheets, and info cards
type: reference
---

# Delta E — Scientific Reference

## Definition
Delta E (ΔE or dE) is a scientific metric that quantifies the difference between two colors, representing how the human eye perceives color variation. It measures the distance between a target color and a sample in the CIE L*a*b* color space, with **lower values indicating smaller (harder to see) differences**.

## Etymology
"Delta" (Δ) = mathematical change. "E" = German word **Empfindung** (sensation/perception).

## Color Space Basis
ΔE uses the **CIELAB (CIE L\*a\*b\*)** color space, which separates:
- **L\*** — Lightness
- **a\*** — Green ↔ Red axis
- **b\*** — Blue ↔ Yellow axis

This space is designed to be perceptually uniform — equal numeric distances correspond to equal perceptual differences.

## Calculation
Fundamentally, ΔE is the Euclidean distance between two points in CIELAB space:
```
ΔE = √(ΔL² + Δa² + Δb²)   [CIE76 — simple, now outdated]
```
Modern formulas (CIEDE2000) are significantly more complex, adding corrections for hue-angle, chroma weighting, and the blue-region hue-chroma interaction.

## Formula Evolution
| Formula | Year | Notes |
|---------|------|-------|
| CIE76 | 1976 | Simple Euclidean; poor match for human perception in saturated colors |
| CMC | 1984 | Better for textile industry |
| CIE94 | 1994 | Improved weighting |
| **CIEDE2000** | **2000** | **Current gold standard** — best match to human visual perception |

**Huezoo uses CIEDE2000** (ISO 11664-6 / CIE 142-2001).

## Perceptual Scale

| ΔE Range | Perception |
|----------|-----------|
| < 1.0 | Not perceptible to the human eye — ideal for professional applications |
| 1.0 – 2.0 | Perceptible only by close observation (trained eye) |
| 2.0 – 3.5 | Perceptible at a glance |
| 3.5 – 5.0 | Clearly different colors |
| > 5.0 | Colors are completely different |

## Real-World Applications
- Paint and coating matching
- Display and monitor calibration
- Textile and fabric color QC
- Medical imaging and diagnostics
- Print production and prepress
- Automotive paint matching

## TODO: Use this content in the app
- [ ] Expand GameHelpSheet "WHAT IS ΔE?" section with scientific depth
- [ ] Add a dedicated "ΔE Science" expandable section in the home screen DeltaEInfoCard
- [ ] Consider adding perceptual scale visualization (color gradient bar from ΔE 0 → 5) in help sheet
- [ ] Add CIE standard reference (ISO 11664-6) in the about/settings section
- [ ] Game copy opportunity: show "X% of humans can't see this" at specific ΔE thresholds
