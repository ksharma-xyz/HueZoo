package xyz.ksharma.huezoo.ui.model

import androidx.compose.ui.graphics.Color
import xyz.ksharma.huezoo.ui.theme.HuezooColors

data class PerceptionTier(
    val rankLabel: String,
    val description: String,
    val deltaERange: String,
    val deltaEMax: Float,
    val color: Color,
)

val PERCEPTION_TIERS = listOf(
    PerceptionTier("TOP 1%", "Near human limits", "ΔE < 0.5", 0.5f, HuezooColors.AccentGold),
    PerceptionTier("TOP 5%", "Professional colorist", "ΔE 0.5 – 1.0", 1.0f, HuezooColors.AccentPink),
    PerceptionTier("TOP 10%", "Trained eye", "ΔE 1.0 – 1.5", 1.5f, HuezooColors.AccentOrange),
    PerceptionTier("TOP 20%", "Designer / photographer", "ΔE 1.5 – 2.0", 2.0f, HuezooColors.AccentGreen),
    PerceptionTier("TOP 40%", "Above average", "ΔE 2.0 – 3.0", 3.0f, HuezooColors.AccentCyan),
    PerceptionTier("TOP 60%", "Average untrained eye", "ΔE 3.0 – 4.0", 4.0f, HuezooColors.TextSecondary),
    PerceptionTier("TOP 80%", "Just starting out", "ΔE > 4.0", Float.MAX_VALUE, HuezooColors.TextDisabled),
)

fun estimatedPerceptionTier(deltaE: Float): PerceptionTier =
    PERCEPTION_TIERS.first { deltaE < it.deltaEMax }
