package xyz.ksharma.huezoo.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import huezoo.composeapp.generated.resources.Res
import huezoo.composeapp.generated.resources.ic_share
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooBodyMedium
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.result.state.ResultUiState
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.ParallelogramBack
import xyz.ksharma.huezoo.ui.theme.darken
import xyz.ksharma.huezoo.ui.theme.onColor
import xyz.ksharma.huezoo.ui.theme.rimLight
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val BannerShape = RoundedCornerShape(12.dp)
private val CardShape = RoundedCornerShape(16.dp)
private val StatIconSize = 28.dp

@Composable
fun ResultScreen(
    result: Result,
    onLeaderboard: () -> Unit,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultViewModel = koinViewModel(parameters = { parametersOf(result) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDaily = result.gameId == GameId.DAILY
    val identityColor = if (isDaily) HuezooColors.GameDaily else HuezooColors.GameThreshold
    val glowColor = if (isDaily) identityColor else HuezooColors.AccentMagenta

    AmbientGlowBackground(
        modifier = modifier,
        primaryColor = glowColor,
        secondaryColor = identityColor,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HuezooTopBar(
                onBackClick = onBack,
                currencyAmount = 0,
            )

            when (val state = uiState) {
                ResultUiState.Loading -> Unit
                is ResultUiState.Ready -> ReadyContent(
                    state = state,
                    onPlayAgain = onPlayAgain,
                )
            }
        }
    }
}

// ── Private helpers ────────────────────────────────────────────────────────────

private data class StingData(val badge: String, val copy: String)

private fun stingData(gameId: String, deltaE: Float, score: Int): StingData = when {
    gameId == GameId.DAILY -> when {
        score > 800 -> StingData("PERFECT RUN", "Perfect run. You see what others miss.")
        score > 500 -> StingData("STRONG SIGNAL", "Strong calibration. Above average perception.")
        score > 200 -> StingData("DRIFTING", "Not bad. Keep training your eye.")
        else -> StingData("SIGNAL LOST", "Try again tomorrow. Your eye will sharpen.")
    }
    deltaE < 0.5f -> StingData("SUPERHUMAN", "Superhuman. This shouldn't be possible.")
    deltaE < 1.0f -> StingData("ELITE SIGNAL", "Your eyes are elite. Top 1%.")
    deltaE < 1.5f -> StingData("SHARP DRIFT", "Sharp. Very sharp. Most quit here.")
    deltaE < 2.0f -> StingData("GOOD SIGNAL", "Better than most. Keep pushing lower.")
    deltaE < 3.0f -> StingData("CRITICAL DRIFT", "ΔE ${deltaE.fmt()} got you. Most people tap out here.")
    else -> StingData("SIGNAL LOST", "Keep training. The signal will come into focus.")
}

private fun Float.fmt(): String {
    val i = toInt()
    val d = ((this - i) * 10).toInt()
    return "$i.$d"
}

// ── ReadyContent ───────────────────────────────────────────────────────────────

@Composable
private fun ReadyContent(
    state: ResultUiState.Ready,
    onPlayAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val platformOps: PlatformOps = koinInject()
    val isDaily = state.gameId == GameId.DAILY
    val identityColor = if (isDaily) HuezooColors.GameDaily else HuezooColors.GameThreshold
    val accentColor = if (isDaily) identityColor else HuezooColors.AccentMagenta
    val sting = stingData(state.gameId, state.deltaE, state.score)
    val shareIcon = painterResource(Res.drawable.ic_share)

    // Count-up: 0 → final score
    val displayScore = remember { Animatable(0f) }
    LaunchedEffect(state.score) {
        displayScore.animateTo(
            state.score.toFloat(),
            spring(stiffness = 80f, dampingRatio = Spring.DampingRatioNoBouncy),
        )
    }

    val shareText = buildString {
        append("I detected ΔE ${state.deltaE.fmt()} on Huezoo\n")
        append(sting.copy)
        append("\nCan you beat it? huezoo.app")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(HuezooSpacing.sm))

        // ── 1. Outcome banner — constrained width, matches stitch ─────────────
        MissionOutcomeBanner(
            text = if (isDaily) "MISSION OUTCOME: COMPLETE" else "MISSION OUTCOME: FAILURE",
            color = accentColor,
        )

        Spacer(Modifier.height(HuezooSpacing.md))

        // ── 2. Hero score ─────────────────────────────────────────────────────
        HeroScore(
            score = displayScore.value.toInt(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(HuezooSpacing.sm))

        // ── 3. ΔE sting readout ───────────────────────────────────────────────
        StingReadout(
            deltaE = state.deltaE,
            badgeText = sting.badge,
            stingCopy = sting.copy,
            accentColor = accentColor,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(HuezooSpacing.sm))

        // ── 4. Stat cards — stacked full-width (matches stitch) ───────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            // SPECTRAL DRIFT — score % toward max
            StatBreakdownCard(
                label = "SPECTRAL DRIFT",
                value = "${((state.score / 5000f) * 100).toInt().coerceIn(0, 100)}%",
                progress = (state.score / 5000f).coerceIn(0f, 1f),
                accentColor = HuezooColors.AccentCyan,
                icon = { WaveIcon(color = HuezooColors.AccentCyan) },
                modifier = Modifier.fillMaxWidth(),
            )
            // CALIBRATION — rounds survived
            StatBreakdownCard(
                label = "CALIBRATION",
                value = "${state.roundsSurvived}",
                progress = (state.roundsSurvived / 15f).coerceIn(0f, 1f),
                accentColor = HuezooColors.AccentYellow,
                icon = { LightningIcon(color = HuezooColors.AccentYellow) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(HuezooSpacing.md))

        // ── 5. Buttons ────────────────────────────────────────────────────────
        HuezooButton(
            text = if (isDaily) "BACK TO HOME" else "PLAY AGAIN",
            onClick = onPlayAgain,
            variant = HuezooButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(HuezooSpacing.sm))
        HuezooButton(
            text = "SHARE SCORE",
            onClick = { platformOps.shareText(shareText) },
            variant = HuezooButtonVariant.GhostDanger,
            leadingIcon = {
                Image(
                    painter = shareIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(HuezooSpacing.md))
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun MissionOutcomeBanner(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .wrapContentWidth()
            .shapedShadow(BannerShape, color.darken(0.5f), offsetX = 0.dp, offsetY = 6.dp)
            .border(2.dp, Color.White.copy(alpha = 0.12f), BannerShape)
            .background(color, BannerShape)
            .padding(horizontal = HuezooSpacing.lg, vertical = HuezooSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        HuezooLabelSmall(
            text = text,
            color = color.onColor,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HeroScore(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 200f),
            )
        }
        launch { alpha.animateTo(1f, tween(250)) }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = "$score PTS",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 88.sp,
                lineHeight = 88.sp,
            ),
            color = HuezooColors.AccentCyan,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StingReadout(
    deltaE: Float,
    badgeText: String,
    stingCopy: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(HuezooColors.SurfaceL2, CardShape)
            .rimLight(cornerRadius = 16.dp)
            .clip(CardShape),
    ) {
        // Left accent strip
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentColor),
        )

        Column(
            modifier = Modifier.padding(HuezooSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            // ΔE value + badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
            ) {
                androidx.compose.material3.Text(
                    text = "ΔE ${deltaE.fmt()}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        lineHeight = 48.sp,
                    ),
                    color = accentColor,
                )
                // Skewed badge
                Box(
                    modifier = Modifier
                        .shapedShadow(ParallelogramBack, accentColor.copy(alpha = 0.3f))
                        .clip(ParallelogramBack)
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    HuezooLabelSmall(
                        text = badgeText,
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // Sting copy
            HuezooBodyMedium(
                text = stingCopy,
                color = HuezooColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun StatBreakdownCard(
    label: String,
    value: String,
    progress: Float,
    accentColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress, tween(900, easing = EaseOutCubic))
    }

    Column(
        modifier = modifier
            .background(HuezooColors.SurfaceL2, CardShape)
            .rimLight(cornerRadius = 16.dp)
            .padding(HuezooSpacing.md),
        verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
    ) {
        // Top row: skewed label chip + icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .shapedShadow(ParallelogramBack, accentColor.copy(alpha = 0.3f))
                    .clip(ParallelogramBack)
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                HuezooLabelSmall(
                    text = label,
                    color = accentColor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            icon()
        }

        // Stat value
        androidx.compose.material3.Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            color = HuezooColors.TextPrimary,
        )

        // Neon progress bar
        NeonProgressBar(
            progress = animatedProgress.value,
            color = accentColor,
        )
    }
}

@Composable
private fun NeonProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val trackShape = RoundedCornerShape(4.dp)
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(HuezooColors.SurfaceL3, trackShape)
            .clip(trackShape),
    ) {
        // Glow halo — semi-transparent wider layer behind solid fill
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .fillMaxHeight()
                .background(color.copy(alpha = 0.35f), trackShape),
        )
        // Solid fill
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .fillMaxHeight(0.6f)
                .align(Alignment.CenterStart)
                .background(color, RoundedCornerShape(4.dp)),
        )
    }
}

// ── Stat card icons ────────────────────────────────────────────────────────────

/** Three horizontal sine-wave lines — used on SPECTRAL DRIFT card. */
@Composable
private fun WaveIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(StatIconSize)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        val gap = h / 4f
        repeat(3) { row ->
            val y = gap * (row + 0.9f)
            val path = Path().apply {
                moveTo(0f, y)
                cubicTo(
                    w * 0.25f,
                    y - gap * 0.55f,
                    w * 0.75f,
                    y + gap * 0.55f,
                    w,
                    y,
                )
            }
            drawPath(path, color, style = stroke)
        }
    }
}

/** Filled lightning bolt — used on CALIBRATION card. */
@Composable
private fun LightningIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(StatIconSize)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.64f, 0f)
            lineTo(w * 0.18f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.50f)
            lineTo(w * 0.36f, h)
            lineTo(w * 0.82f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.50f)
            close()
        }
        drawPath(path, color)
    }
}
