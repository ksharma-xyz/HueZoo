package xyz.ksharma.huezoo.ui.model

/**
 * Visual arrangement style for a round of The Threshold (and any future screen that uses
 * [RadialSwatchLayout]).
 *
 * Each entry maps to a distinct tile [Shape] and radial geometry.  A random style (never the
 * same as the previous round) is chosen by the ViewModel at the start of each round so the
 * layout feels fresh on every attempt.
 *
 * All styles place exactly 6 tiles around a shared centre point with a configurable gap,
 * so the game mechanic (tap the odd colour) is identical regardless of shape.
 */
enum class SwatchLayoutStyle {

    /**
     * 6 petal/teardrop shapes blooming outward — the flower.
     * Petals taper to a point near the centre, fan wide at the outer edge.
     */
    Flower,

    /**
     * 6 flat-top hexagons orbiting the centre — a honeycomb ring.
     * Uniform, tightly-packed, feels structural.
     */
    HexRing,

    /**
     * 6 squircles in a soft orbit — minimal and clean.
     * Familiar rounded-square form, spaced evenly.
     */
    SquircleOrbit,

    /**
     * 6 tall, narrow blades radiating outward like wheel spokes.
     * Industrial / tyre-rim aesthetic; blades extend from a gap near centre.
     */
    SpokeBlades,

    /**
     * 6 diamond (rotated-square) shapes forming a pointed halo.
     * Each diamond's top tip points toward the centre; bottom tip fans outward.
     */
    DiamondHalo,

    /**
     * 6 rounded shield / badge shapes — pointed inner tip, wide convex outer body with
     * concave "shoulder" curves.  Inspired by [docs/ideas/swatch_diamong.png].
     */
    ShieldRing,

    /**
     * 6 citrus-slice crescents — concave inner arc facing the centre, convex outer arc
     * facing outward, tips at the sides.  Landscape tile (wider than tall).
     * Inspired by [docs/ideas/swatch_fruit.png].
     */
    CitrusSlice,

    /**
     * 6 long carrot tapers — frilly leafy inner edge (zigzag peaks) close to the centre,
     * narrow pointed root tip at the outer end.
     */
    Carrot,

    /**
     * 6 pentagonal arrowheads pointing inward, with a chevron notch carved into the outer
     * edge.  Combined radially they form an alloy-wheel pattern with twin-spoke overlap.
     * Pairs with an entrance-spin animation that rotates each tile into place like a wheel.
     */
    AlloyArrow,

    /**
     * 6 twin-fork "Y" alloy spokes — narrow stem at the inner edge widening into two prongs
     * separated by a deep V-notch at the outer edge.  Six tiles → 12 outer prongs around a
     * tight hub.  Audi-style Y-spoke alloy.
     */
    YSpoke,

    /**
     * 6 three-prong trident alloys — single inner tip, two V-notches carving the outer edge
     * into three flat prongs.  Mercedes-AMG split-spoke aesthetic.
     */
    TridentSpoke,

    /**
     * 6 chevron arrowheads with concave (cutout) outer-side edges — gives a sharp jet-wing
     * silhouette.  Same 6-corner anchor template as AlloyArrow but with curved sweeps
     * between corners instead of straight lines.
     */
    CutoutChevron,

    /**
     * 6 Mjolnir-hammer silhouettes — narrow handle with a rounded grip at the inner edge,
     * widening into a chamfered hammer head that faces outward.
     */
    Mjolnir,
}
