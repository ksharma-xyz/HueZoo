package xyz.ksharma.huezoo.ui.model

/**
 * Three-tier player level system based on lifetime gems earned
 * (1 gem ≈ 1 correct answer, as per the economy spec).
 *
 * Thresholds proxy the correct-answer counts from the spec:
 *   Rookie: 0–199 correct  → 0–399 gems
 *   Skilled: 200–49,999    → 400–99,999 gems
 *   Master: 50,000+        → 100,000+ gems
 *
 * Colors: Cyan (L1) / Magenta (L2) / Gold (L3) — per IMPLEMENTATION_PLAN_LEVELS_ECONOMY_UX.md
 */
enum class PlayerLevel(
    val displayName: String,
    val minGems: Int,
) {
    Rookie(displayName = "ROOKIE", minGems = 0),
    Skilled(displayName = "SKILLED", minGems = 400),
    Master(displayName = "MASTER", minGems = 100_000);

    companion object {
        fun fromGems(gems: Int): PlayerLevel = when {
            gems >= 100_000 -> Master
            gems >= 400 -> Skilled
            else -> Rookie
        }
    }
}
