package xyz.ksharma.huezoo.ui.model

/**
 * Visual celebration tiers for correct taps in threshold games.
 *
 * Each level is fully self-describing via [CelebrationConfig] — the burst composable
 * ([CelebrationBurstOverlay]) is driven entirely by data, so adding a new tier only
 * requires adding a new object here.
 *
 * ## Threshold rules (ThresholdViewModel controls when each fires)
 * - [Sharp]  : first correct tap at ΔE ≤ [THRESHOLD_SHARP] per try  → small fan burst
 * - [Expert] : first correct tap at ΔE ≤ [THRESHOLD_EXPERT] per try → wide fan burst
 * - [Massive]: **every** correct tap at ΔE ≤ [THRESHOLD_MASSIVE]    → full radial explosion
 */
sealed class CelebrationLevel(val config: CelebrationConfig) {

    /** Small upward fan — ΔE ≤ [THRESHOLD_SHARP], once per try. */
    @Suppress("MagicNumber")
    data object Sharp : CelebrationLevel(
        CelebrationConfig(
            particleCount = 10,
            maxRadiusDp = 5f,
            minRadiusDp = 2f,
            travelDistanceDp = 80f,
            durationMs = 1_200,
            coneHalfDeg = 50f,
            maxAlpha = 0.70f,
            centerSpawn = false,
        ),
    )

    /** Wide fan — ΔE ≤ [THRESHOLD_EXPERT], once per try. */
    @Suppress("MagicNumber")
    data object Expert : CelebrationLevel(
        CelebrationConfig(
            particleCount = 22,
            maxRadiusDp = 9f,
            minRadiusDp = 3f,
            travelDistanceDp = 120f,
            durationMs = 1_800,
            coneHalfDeg = 100f,
            maxAlpha = 0.88f,
            centerSpawn = false,
        ),
    )

    /** Full radial explosion — ΔE ≤ [THRESHOLD_MASSIVE], fires on every correct tap. */
    @Suppress("MagicNumber")
    data object Massive : CelebrationLevel(
        CelebrationConfig(
            particleCount = 38,
            maxRadiusDp = 14f,
            minRadiusDp = 4f,
            travelDistanceDp = 160f,
            durationMs = 2_400,
            coneHalfDeg = 180f,
            maxAlpha = 1.0f,
            centerSpawn = true,
        ),
    )

    companion object {
        // ΔE boundaries that control when each tier fires (owned here so ViewModel
        // and any other game engine can import them from a single place).
        const val THRESHOLD_SHARP = 3.0f
        const val THRESHOLD_EXPERT = 2.0f
        const val THRESHOLD_MASSIVE = 1.3f
    }
}

/**
 * Visual parameters for a single celebration burst animation.
 *
 * @param particleCount     Total number of particles in the burst.
 * @param maxRadiusDp       Largest circle radius in dp.
 * @param minRadiusDp       Smallest circle radius in dp.
 * @param travelDistanceDp  Maximum distance a particle travels from its spawn point in dp.
 * @param durationMs        Total animation duration in milliseconds.
 * @param coneHalfDeg       Half-angle of the directional spread cone. 50° = tight upward fan;
 *                          180° = full 360° radial burst.
 * @param maxAlpha          Peak alpha for the brightest particles (0–1).
 * @param centerSpawn       `true` → all particles spawn near canvas centre (good for radial bursts).
 *                          `false` → spawn positions spread randomly across the canvas area.
 */
data class CelebrationConfig(
    val particleCount: Int,
    val maxRadiusDp: Float,
    val minRadiusDp: Float,
    val travelDistanceDp: Float,
    val durationMs: Int,
    val coneHalfDeg: Float,
    val maxAlpha: Float,
    val centerSpawn: Boolean,
)
