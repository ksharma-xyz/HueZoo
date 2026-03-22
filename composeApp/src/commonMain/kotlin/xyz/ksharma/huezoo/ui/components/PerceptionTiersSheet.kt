package xyz.ksharma.huezoo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.model.PERCEPTION_TIERS
import xyz.ksharma.huezoo.ui.model.PerceptionTier
import xyz.ksharma.huezoo.ui.model.estimatedPerceptionTier
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

/**
 * Bottom sheet displaying the full ΔE perception tier ladder.
 *
 * - Shows for both ranked (has personal best) and unranked players.
 * - Highlights the player's current tier if they have a personal best.
 * - Explains what each tier means in plain language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerceptionTiersSheet(
    personalBestDeltaE: Float?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTier = personalBestDeltaE?.let { estimatedPerceptionTier(it) }

    HuezooBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HuezooSpacing.md)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.xs),
        ) {
            // Header
            HuezooLabelSmall(
                text = "COLOR PERCEPTION TIERS",
                color = HuezooColors.TextDisabled,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(Modifier.height(HuezooSpacing.xs))

            // Current classification card (if ranked)
            if (personalBestDeltaE != null && currentTier != null) {
                ClassificationBanner(
                    deltaE = personalBestDeltaE,
                    tier = currentTier,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(HuezooSpacing.sm))
            } else {
                UnrankedBanner(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(HuezooSpacing.sm))
            }

            // Tier rows
            PERCEPTION_TIERS.forEach { tier ->
                PerceptionTierRow(
                    tier = tier,
                    isCurrentTier = tier == currentTier,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(HuezooSpacing.md))

            // Footer note
            HuezooLabelSmall(
                text = "Measured using CIEDE2000 — the international standard for human color difference perception.",
                color = HuezooColors.TextDisabled,
                maxLines = Int.MAX_VALUE,
            )

            Spacer(Modifier.height(HuezooSpacing.lg))
        }
    }
}

// ── Classification banner (ranked) ───────────────────────────────────────────

@Composable
private fun ClassificationBanner(
    deltaE: Float,
    tier: PerceptionTier,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(HuezooSize.CornerCard))
            .background(HuezooColors.SurfaceL4)
            .drawBehind {
                val barWidth = 3.dp.toPx()
                drawRect(
                    color = tier.color,
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, size.height),
                )
            }
            .padding(
                start = HuezooSpacing.md + 3.dp,
                end = HuezooSpacing.md,
                top = HuezooSpacing.sm,
                bottom = HuezooSpacing.sm,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HuezooLabelSmall(
                    text = "YOUR CLASSIFICATION",
                    color = HuezooColors.TextDisabled,
                    fontWeight = FontWeight.ExtraBold,
                )
                HuezooTitleLarge(
                    text = tier.rankLabel,
                    color = tier.color,
                    fontWeight = FontWeight.ExtraBold,
                )
                HuezooLabelSmall(
                    text = tier.description,
                    color = HuezooColors.TextSecondary,
                )
            }

            HuezooTitleLarge(
                text = deltaE.fmtTier(),
                color = tier.color,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = HuezooSpacing.sm),
            )
        }
    }
}

// ── Unranked banner ───────────────────────────────────────────────────────────

@Composable
private fun UnrankedBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(HuezooSize.CornerCard))
            .background(HuezooColors.SurfaceL4)
            .drawBehind {
                val barWidth = 3.dp.toPx()
                drawRect(
                    color = HuezooColors.TextDisabled,
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, size.height),
                )
            }
            .padding(
                start = HuezooSpacing.md + 3.dp,
                end = HuezooSpacing.md,
                top = HuezooSpacing.sm,
                bottom = HuezooSpacing.sm,
            ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            HuezooLabelSmall(
                text = "YOUR CLASSIFICATION",
                color = HuezooColors.TextDisabled,
                fontWeight = FontWeight.ExtraBold,
            )
            HuezooTitleMedium(
                text = "UNRANKED",
                color = HuezooColors.TextDisabled,
            )
            HuezooLabelSmall(
                text = "Play The Threshold to discover your tier.",
                color = HuezooColors.TextSecondary,
                maxLines = 2,
            )
        }
    }
}

// ── Single tier row ───────────────────────────────────────────────────────────

@Composable
private fun PerceptionTierRow(
    tier: PerceptionTier,
    isCurrentTier: Boolean,
    modifier: Modifier = Modifier,
) {
    val rowModifier = if (isCurrentTier) {
        modifier
            .clip(RoundedCornerShape(HuezooSize.CornerCard))
            .background(HuezooColors.SurfaceL4)
            .padding(HuezooSpacing.sm)
    } else {
        modifier.padding(horizontal = HuezooSpacing.sm, vertical = HuezooSpacing.xs)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
    ) {
        // Colored vertical bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tier.color),
        )

        // Label + description
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            HuezooLabelSmall(
                text = tier.rankLabel,
                color = tier.color,
                fontWeight = FontWeight.ExtraBold,
            )
            HuezooLabelSmall(
                text = tier.description,
                color = HuezooColors.TextSecondary,
                maxLines = Int.MAX_VALUE,
            )
        }

        // ΔE range
        HuezooLabelSmall(
            text = tier.deltaERange,
            color = if (isCurrentTier) tier.color.copy(alpha = 0.7f) else HuezooColors.TextDisabled,
        )
    }
}

private fun Float.fmtTier(): String {
    val i = toInt()
    val d = ((this - i) * DECIMAL_SCALE).toInt()
    return "ΔE $i.$d"
}

private const val DECIMAL_SCALE = 10
