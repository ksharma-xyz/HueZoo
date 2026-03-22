@file:Suppress("MatchingDeclarationName") // File intentionally contains multiple private composables

package xyz.ksharma.huezoo.ui.leaderboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.ui.components.HuezooBodyMedium
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooDisplayLarge
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTitleLarge
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.model.PERCEPTION_TIERS
import xyz.ksharma.huezoo.ui.model.PerceptionTier
import xyz.ksharma.huezoo.ui.model.estimatedPerceptionTier
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val DECIMAL_SCALE = 10

private fun Float.fmt(): String {
    val i = toInt()
    val d = ((this - i) * DECIMAL_SCALE).toInt()
    return "$i.$d"
}

// ── Screen entry point ────────────────────────────────────────────────────────

@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    onShare: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LeaderboardViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LeaderboardContent(
        personalBestDeltaE = state.personalBestDeltaE,
        onBack = onBack,
        onShare = onShare,
        modifier = modifier,
    )
}

// ── Content ───────────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardContent(
    personalBestDeltaE: Float?,
    onBack: () -> Unit,
    onShare: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tier = personalBestDeltaE?.let { estimatedPerceptionTier(it) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HuezooColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // Top bar
        HuezooTopBar(onBackClick = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HuezooSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
        ) {
            Spacer(Modifier.height(HuezooSpacing.sm))

            // Signal offline pulsing indicator
            SignalOfflineRow()

            // Sonar animation
            SonarCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )

            // Agent Classification card
            AgentClassificationCard(
                personalBestDeltaE = personalBestDeltaE,
                tier = tier,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(HuezooSpacing.xs))

            // Activation Protocol section label
            HuezooLabelSmall(
                text = "ACTIVATION PROTOCOL",
                color = HuezooColors.TextDisabled,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = HuezooSpacing.xs),
            )

            // Activation protocol box
            ActivationProtocolCard(modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(HuezooSpacing.xs))

            // Perception Tiers section label
            HuezooLabelSmall(
                text = "PERCEPTION TIERS",
                color = HuezooColors.TextDisabled,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = HuezooSpacing.xs),
            )

            // Tier rows
            Column(
                verticalArrangement = Arrangement.spacedBy(HuezooSpacing.xs),
            ) {
                PERCEPTION_TIERS.forEach { rankTier ->
                    TierRow(
                        rankTier = rankTier,
                        isCurrentTier = rankTier == tier,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(HuezooSpacing.xs))

            // Share button
            val shareText = if (personalBestDeltaE != null && tier != null) {
                "I'm classified as ${tier.rankLabel} in color perception on Huezoo — " +
                    "my ΔE score is ${personalBestDeltaE.fmt()}. Can your eyes beat mine? " +
                    "Download Huezoo and find out."
            } else {
                "I just discovered Huezoo — a game that tests how precisely you can see color. " +
                    "My eyes are being calibrated. Join me. Download Huezoo."
            }

            HuezooButton(
                text = "BROADCAST SIGNAL",
                onClick = { onShare(shareText) },
                variant = HuezooButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(HuezooSpacing.lg))
        }
    }
}

// ── Signal offline pulsing row ────────────────────────────────────────────────

@Composable
private fun SignalOfflineRow(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "signalPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(
                color = HuezooColors.AccentMagenta.copy(alpha = pulseAlpha),
                radius = size.minDimension / 2f,
            )
        }
        Spacer(Modifier.width(HuezooSpacing.xs))
        HuezooLabelSmall(
            text = "SIGNAL OFFLINE",
            color = HuezooColors.AccentMagenta.copy(alpha = pulseAlpha),
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

// ── Sonar animation ───────────────────────────────────────────────────────────

@Composable
private fun SonarCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonar")

    val ring0 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
        ),
        label = "ring0",
    )
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            initialStartOffset = StartOffset(offsetMillis = 667),
        ),
        label = "ring1",
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            initialStartOffset = androidx.compose.animation.core.StartOffset(offsetMillis = 1334),
        ),
        label = "ring2",
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = minOf(size.width, size.height) / 2f
        val strokeWidth = 1.5.dp.toPx()
        val dotRadius = 5.dp.toPx()
        val ringColor = HuezooColors.AccentCyan

        // Draw rings
        for (prog in listOf(ring0, ring1, ring2)) {
            val radius = prog * maxRadius
            val alpha = (1f - prog) * 0.55f
            drawCircle(
                color = ringColor.copy(alpha = alpha),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = strokeWidth),
            )
        }

        // Sweep arm — rotates once per 2000ms cycle using ring0 progress
        val sweepAngleRad = (ring0 * 360f - 90f) * (PI / 180f)
        val armLength = maxRadius * 0.6f
        drawLine(
            color = ringColor.copy(alpha = 0.3f),
            start = Offset(cx, cy),
            end = Offset(
                cx + (armLength * cos(sweepAngleRad)).toFloat(),
                cy + (armLength * sin(sweepAngleRad)).toFloat(),
            ),
            strokeWidth = strokeWidth,
        )

        // Center dot
        drawCircle(
            color = ringColor,
            radius = dotRadius,
            center = Offset(cx, cy),
        )
    }
}

// ── Agent Classification card ─────────────────────────────────────────────────

@Composable
private fun AgentClassificationCard(
    personalBestDeltaE: Float?,
    tier: PerceptionTier?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(HuezooSize.CornerCard))
            .background(HuezooColors.SurfaceL2)
            .drawBehind {
                val barWidth = 3.dp.toPx()
                drawRect(
                    color = (tier?.color ?: HuezooColors.TextDisabled),
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, size.height),
                )
            }
            .padding(
                start = HuezooSpacing.md + 3.dp,
                end = HuezooSpacing.md,
                top = HuezooSpacing.md,
                bottom = HuezooSpacing.md,
            ),
    ) {
        if (personalBestDeltaE != null && tier != null) {
            Column(verticalArrangement = Arrangement.spacedBy(HuezooSpacing.xs)) {
                HuezooLabelSmall(
                    text = "AGENT CLASSIFICATION",
                    color = tier.color,
                    fontWeight = FontWeight.ExtraBold,
                )
                HuezooDisplayLarge(
                    text = tier.rankLabel,
                    color = tier.color,
                    fontWeight = FontWeight.ExtraBold,
                )
                HuezooLabelSmall(
                    text = tier.description,
                    color = HuezooColors.TextSecondary,
                    maxLines = Int.MAX_VALUE,
                )
                HuezooLabelSmall(
                    text = "Based on personal best ΔE ${personalBestDeltaE.fmt()}",
                    color = HuezooColors.TextDisabled,
                    maxLines = Int.MAX_VALUE,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(HuezooSpacing.xs)) {
                HuezooLabelSmall(
                    text = "AGENT CLASSIFICATION",
                    color = HuezooColors.TextDisabled,
                    fontWeight = FontWeight.ExtraBold,
                )
                HuezooTitleLarge(
                    text = "UNRANKED",
                    color = HuezooColors.TextDisabled,
                )
                HuezooBodyMedium(
                    text = "Play The Threshold to receive your classification.",
                    color = HuezooColors.TextSecondary,
                    maxLines = Int.MAX_VALUE,
                )
            }
        }
    }
}

// ── Tier row ──────────────────────────────────────────────────────────────────

@Composable
private fun TierRow(
    rankTier: PerceptionTier,
    isCurrentTier: Boolean,
    modifier: Modifier = Modifier,
) {
    val rowModifier = if (isCurrentTier) {
        modifier
            .clip(RoundedCornerShape(HuezooSize.CornerCard))
            .background(HuezooColors.SurfaceL2)
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
                .background(rankTier.color),
        )

        // Label + description
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            HuezooLabelSmall(
                text = rankTier.rankLabel,
                color = rankTier.color,
                fontWeight = FontWeight.ExtraBold,
            )
            HuezooLabelSmall(
                text = rankTier.description,
                color = HuezooColors.TextSecondary,
                maxLines = Int.MAX_VALUE,
            )
        }

        // Delta E range
        HuezooLabelSmall(
            text = rankTier.deltaERange,
            color = HuezooColors.TextDisabled,
        )
    }
}

// ── Activation Protocol card ──────────────────────────────────────────────────

@Composable
private fun ActivationProtocolCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(HuezooSize.CornerCard))
            .background(HuezooColors.SurfaceL2)
            .padding(HuezooSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
            // Leaderboard status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HuezooLabelSmall(
                    text = "LEADERBOARD STATUS",
                    color = HuezooColors.TextDisabled,
                )
                HuezooLabelSmall(
                    text = "OFFLINE",
                    color = HuezooColors.AccentMagenta,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            // Operator threshold row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HuezooLabelSmall(
                    text = "OPERATOR THRESHOLD",
                    color = HuezooColors.TextDisabled,
                )
                HuezooLabelSmall(
                    text = "500 AGENTS",
                    color = HuezooColors.AccentCyan,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            // Progress bar with dots
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HuezooColors.SurfaceL4),
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    val dotRadius = 4.dp.toPx()
                    val dotCount = 4
                    for (i in 0 until dotCount) {
                        val fraction = (i + 1f) / (dotCount + 1f)
                        drawCircle(
                            color = HuezooColors.AccentCyan.copy(alpha = 0.4f),
                            radius = dotRadius,
                            center = Offset(size.width * fraction, size.height / 2f),
                        )
                    }
                }
            }

            // Description
            HuezooBodyMedium(
                text = "Global rankings go live when 500 operators enroll. Recruit agents to accelerate activation.",
                color = HuezooColors.TextSecondary,
                maxLines = Int.MAX_VALUE,
            )
        }
    }
}
