package xyz.ksharma.huezoo.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
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
import xyz.ksharma.huezoo.ui.theme.darken
import xyz.ksharma.huezoo.ui.theme.onColor
import xyz.ksharma.huezoo.ui.theme.rimLight
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val BannerShape = RoundedCornerShape(12.dp)
private val CardShape = RoundedCornerShape(16.dp)
private val StatIconSize = 28.dp
private val CardShelfHeight = 4.dp

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

private fun Int.formatWithCommas(): String {
    if (this < 1000) return toString()
    val s = toString()
    val result = StringBuilder()
    s.reversed().forEachIndexed { i, c ->
        if (i > 0 && i % 3 == 0) result.append(',')
        result.append(c)
    }
    return result.reversed().toString()
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

        // ── 1. Outcome banner — full-width, colored shelf ──────────────────────
        MissionOutcomeBanner(
            text = if (isDaily) "MISSION OUTCOME: COMPLETE" else "MISSION OUTCOME: FAILURE",
            color = accentColor,
        )

        Spacer(Modifier.height(HuezooSpacing.md))

        // ── 2. Hero score — ambient radial glow behind ─────────────────────────
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

        // ── 4. Stat cards — stacked full-width, each with bottom shelf ─────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            StatBreakdownCard(
                label = "SPECTRAL DRIFT",
                value = "${((state.score / 5000f) * 100).toInt().coerceIn(0, 100)}%",
                progress = (state.score / 5000f).coerceIn(0f, 1f),
                accentColor = HuezooColors.AccentCyan,
                icon = { WaveIcon(color = HuezooColors.AccentCyan) },
                modifier = Modifier.fillMaxWidth(),
            )
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

        // ── 5. Buttons — PLAY AGAIN + share icon in same row ─────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HuezooButton(
                text = if (isDaily) "BACK TO HOME" else "PLAY AGAIN",
                onClick = onPlayAgain,
                variant = HuezooButtonVariant.Primary,
                leadingIcon = if (!isDaily) {
                    { PlayIcon() }
                } else {
                    null
                },
                modifier = Modifier.weight(1f),
            )
            ShareIconButton(
                onClick = { platformOps.shareText(shareText) },
                icon = shareIcon,
            )
        }

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
            .fillMaxWidth()
            .shapedShadow(BannerShape, color.darken(0.5f), offsetX = 0.dp, offsetY = 6.dp)
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
            .drawBehind {
                // Soft radial ambient glow behind the score text (matches stitch bg-primary/5 blur-3xl)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HuezooColors.AccentCyan.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.9f,
                    ),
                )
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = "${score.formatWithCommas()} PTS",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                lineHeight = 96.sp,
                fontStyle = FontStyle.Italic,
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
    Box(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(HuezooColors.SurfaceL1, CardShape)
            .rimLight(cornerRadius = 16.dp)
            .clip(CardShape),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(HuezooSpacing.md),
                verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
            ) {
                // ΔE value + inline badge — badge aligns to the ΔE text baseline
                Row(
                    horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
                ) {
                    androidx.compose.material3.Text(
                        text = "ΔE ${deltaE.fmt()}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 56.sp,
                            lineHeight = 56.sp,
                            fontStyle = FontStyle.Italic,
                        ),
                        color = accentColor,
                        modifier = Modifier.alignByBaseline(),
                    )
                    HuezooLabelSmall(
                        text = badgeText,
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.alignByBaseline(),
                    )
                }

                // Sting copy — left-aligned, capped to 80% width so the ghost icon stays visible
                HuezooBodyMedium(
                    text = stingCopy,
                    color = HuezooColors.TextSecondary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(0.82f),
                )
            }
        }

        // Ghost watermark icon — top-right corner, 10% opacity
        GhostExclamationIcon(
            color = accentColor.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(HuezooSpacing.sm),
        )
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

    // Outer Box reserves space for the bottom shelf
    Box(modifier = modifier.padding(bottom = CardShelfHeight)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shapedShadow(CardShape, HuezooColors.SurfaceL0, offsetX = 0.dp, offsetY = CardShelfHeight)
                .background(HuezooColors.SurfaceL3, CardShape)
                .rimLight(cornerRadius = 16.dp)
                .padding(HuezooSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            // Top row: solid filled label chip + icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Solid filled parallelogram label chip (matches stitch bg-primary / bg-tertiary)
                Box(
                    modifier = Modifier
                        .shapedShadow(
                            xyz.ksharma.huezoo.ui.theme.ParallelogramBack,
                            accentColor.darken(0.5f),
                        )
                        .clip(xyz.ksharma.huezoo.ui.theme.ParallelogramBack)
                        .background(accentColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    HuezooLabelSmall(
                        text = label,
                        color = accentColor.onColor,
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
            .background(HuezooColors.SurfaceL0, trackShape)
            .clip(trackShape),
    ) {
        // Glow halo
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

// ── Icon button ────────────────────────────────────────────────────────────────

private val ShareButtonSize = 52.dp
private val ShareButtonShelfHeight = 5.dp
private val ShareButtonShape = RoundedCornerShape(16.dp)

/** Square icon button that matches the HuezooButton press-shelf visual style. */
@Composable
private fun ShareIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.painter.Painter,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = if (isPressed) {
            tween(80)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "sharePress",
    )
    val shelfPx = with(LocalDensity.current) { ShareButtonShelfHeight.toPx() }

    Box(modifier = modifier.padding(bottom = ShareButtonShelfHeight)) {
        Box(
            modifier = Modifier
                .size(ShareButtonSize)
                .graphicsLayer {
                    translationY = pressProgress * shelfPx
                    clip = false
                }
                .shapedShadow(
                    ShareButtonShape,
                    HuezooColors.ShelfMagenta,
                    offsetX = 0.dp,
                    offsetY = ShareButtonShelfHeight,
                )
                .border(1.dp, HuezooColors.AccentMagenta, ShareButtonShape)
                .background(Color.Transparent, ShareButtonShape)
                .clip(ShareButtonShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = icon,
                contentDescription = "Share score",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Icons ──────────────────────────────────────────────────────────────────────

/** Three sine-wave lines — SPECTRAL DRIFT card. */
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
                cubicTo(w * 0.25f, y - gap * 0.55f, w * 0.75f, y + gap * 0.55f, w, y)
            }
            drawPath(path, color, style = stroke)
        }
    }
}

/** Filled lightning bolt — CALIBRATION card. */
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

/** Solid filled right-pointing triangle — used as a play icon on the PLAY AGAIN button. */
@Composable
private fun PlayIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(16.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, HuezooColors.AccentCyan.onColor)
    }
}

/** Decorative ghost exclamation mark — drawn as a large "!" at low alpha. */
@Composable
private fun GhostExclamationIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(64.dp)) {
        val cx = size.width / 2f
        val stemW = size.width * 0.14f
        val stemTop = size.height * 0.08f
        val stemBot = size.height * 0.62f
        val dotTop = size.height * 0.72f
        val dotBot = size.height * 0.92f

        // Stem
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(cx - stemW / 2f, stemTop),
            size = Size(stemW, stemBot - stemTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stemW / 2f),
        )
        // Dot
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(cx - stemW / 2f, dotTop),
            size = Size(stemW, dotBot - dotTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stemW / 2f),
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun ResultThresholdFailurePreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        ReadyContent(
            state = xyz.ksharma.huezoo.ui.result.state.ResultUiState.Ready(
                gameId = xyz.ksharma.huezoo.navigation.GameId.THRESHOLD,
                deltaE = 1.4f,
                score = 714,
                roundsSurvived = 4,
                isNewPersonalBest = false,
                personalBestDeltaE = 1.2f,
            ),
            onPlayAgain = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun ResultThresholdElitePreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        ReadyContent(
            state = xyz.ksharma.huezoo.ui.result.state.ResultUiState.Ready(
                gameId = xyz.ksharma.huezoo.navigation.GameId.THRESHOLD,
                deltaE = 0.8f,
                score = 1250,
                roundsSurvived = 11,
                isNewPersonalBest = true,
                personalBestDeltaE = 0.8f,
            ),
            onPlayAgain = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun ResultDailyCompletePreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        ReadyContent(
            state = xyz.ksharma.huezoo.ui.result.state.ResultUiState.Ready(
                gameId = xyz.ksharma.huezoo.navigation.GameId.DAILY,
                deltaE = 2.1f,
                score = 476,
                roundsSurvived = 1,
                isNewPersonalBest = false,
                personalBestDeltaE = null,
            ),
            onPlayAgain = {},
        )
    }
}
