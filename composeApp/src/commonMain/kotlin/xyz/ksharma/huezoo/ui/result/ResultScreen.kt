package xyz.ksharma.huezoo.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.GemAward
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.platform.shareIconRes
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooBodyMedium
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.result.state.ResultUiState
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.darken
import xyz.ksharma.huezoo.ui.theme.onColor
import xyz.ksharma.huezoo.ui.theme.rimLight
import xyz.ksharma.huezoo.ui.theme.shapedShadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val BannerShape = RoundedCornerShape(12.dp)
private val CardShape = RoundedCornerShape(16.dp)
private val StatIconSize = 28.dp
private val CardShelfHeight = 4.dp

@Composable
fun ResultScreen(
    gameId: String,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onUpgradeTap: () -> Unit = {},
    viewModel: ResultViewModel = koinViewModel(),
) {
    val platformOps: PlatformOps = koinInject()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDaily = gameId == GameId.DAILY
    val identityColor = if (isDaily) HuezooColors.GameDaily else HuezooColors.GameThreshold
    val glowColor = if (isDaily) identityColor else HuezooColors.AccentMagenta

    Box(modifier = modifier) {
        AmbientGlowBackground(
            modifier = Modifier.fillMaxSize(),
            primaryColor = glowColor,
            secondaryColor = identityColor,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HuezooTopBar(
                    onBackClick = onBack,
                    currencyAmount = null,
                )

                when (val state = uiState) {
                    ResultUiState.Loading -> Unit
                    is ResultUiState.Ready -> ReadyContent(
                        state = state,
                        onPlayAgain = onPlayAgain,
                        onShare = { text -> platformOps.shareText(text) },
                        onUpgradeTap = onUpgradeTap,
                    )
                }
            }
        }

        // Confetti overlay — gems earned OR new personal best
        val readyState = uiState as? ResultUiState.Ready
        if (readyState != null && (readyState.gemsEarned > 0 || readyState.isNewPersonalBest)) {
            ConfettiEffect(
                identityColor = identityColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ── Private helpers ────────────────────────────────────────────────────────────

private data class StingData(val badge: String, val copy: String)

private fun stingData(gameId: String, deltaE: Float, roundsSurvived: Int): StingData = when {
    gameId == GameId.DAILY -> when {
        roundsSurvived == 6 -> StingData("PERFECT RUN", "Perfect run. You see what others miss.")
        roundsSurvived >= 4 -> StingData("STRONG SIGNAL", "Strong calibration. Above average perception.")
        roundsSurvived >= 2 -> StingData("DRIFTING", "Not bad. Keep training your eye.")
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
    onShare: (String) -> Unit,
    onUpgradeTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDaily = state.gameId == GameId.DAILY
    val identityColor = if (isDaily) HuezooColors.GameDaily else HuezooColors.GameThreshold
    val accentColor = if (isDaily) identityColor else HuezooColors.AccentMagenta
    val sting = stingData(state.gameId, state.deltaE, state.roundsSurvived)
    val shareIcon = painterResource(shareIconRes())

    // Slide-up entrance: content rises from 60dp below + fades in
    val slideUp = remember { Animatable(60f) }
    val fadeIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            slideUp.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 200f),
            )
        }
        launch { fadeIn.animateTo(1f, tween(300)) }
    }

    // Count-up: 0 → gemsEarned
    val displayGems = remember { Animatable(0f) }
    LaunchedEffect(state.gemsEarned) {
        displayGems.animateTo(
            state.gemsEarned.toFloat(),
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
            .graphicsLayer {
                translationY = slideUp.value
                alpha = fadeIn.value
            }
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(HuezooSpacing.sm))

        // ── 1. Outcome banner — full-width, colored shelf ──────────────────────
        val bannerText = when {
            state.roundsSurvived == 0 -> "MISSION OUTCOME: FLATLINED"
            isDaily -> "MISSION OUTCOME: COMPLETE"
            else -> "MISSION OUTCOME: FAILURE"
        }
        val bannerColor = if (state.roundsSurvived == 0) HuezooColors.AccentMagenta else accentColor
        MissionOutcomeBanner(
            text = bannerText,
            color = bannerColor,
        )

        Spacer(Modifier.height(HuezooSpacing.md))

        // ── 2. Hero gems — animated count-up, level accent ────────────────────
        HeroGems(
            gems = displayGems.value.toInt(),
            color = if (state.gemsEarned == 0) HuezooColors.AccentMagenta else LocalPlayerAccentColor.current,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(HuezooSpacing.sm))

        // ── NEW PERSONAL BEST banner ──────────────────────────────────────────
        if (state.isNewPersonalBest) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HuezooColors.AccentYellow.copy(alpha = 0.15f), BannerShape)
                    .border(1.dp, HuezooColors.AccentYellow.copy(alpha = 0.6f), BannerShape)
                    .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                HuezooLabelSmall(
                    text = "★  NEW PERSONAL BEST",
                    color = HuezooColors.AccentYellow,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(HuezooSpacing.sm))
        }

        // ── 3. ΔE sting readout ───────────────────────────────────────────────
        StingReadout(
            deltaE = state.deltaE,
            badgeText = sting.badge,
            stingCopy = sting.copy,
            accentColor = accentColor,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(HuezooSpacing.sm))

        // ── 4. Stat cards ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            val statAccent = LocalPlayerAccentColor.current

            // Rounds correct — shown for both game types
            if (state.totalRounds > 0) {
                val roundsLabel = if (isDaily) "ROUNDS CORRECT" else "CORRECT ROUNDS"
                StatBreakdownCard(
                    label = roundsLabel,
                    value = "${state.correctRounds} / ${state.totalRounds}",
                    progress = (state.correctRounds.toFloat() / state.totalRounds).coerceIn(0f, 1f),
                    accentColor = statAccent,
                    icon = { WaveIcon(color = statAccent) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Threshold-only: all-time best ΔE (session ΔE already shown in StingReadout)
            if (!isDaily) {
                val lifetimeBest = state.personalBestDeltaE
                StatBreakdownCard(
                    label = "ALL-TIME BEST ΔE",
                    value = if (lifetimeBest != null) "ΔE ${lifetimeBest.fmt()}" else "—",
                    progress = if (lifetimeBest != null) (1f - (lifetimeBest / 5f)).coerceIn(0f, 1f) else 0f,
                    accentColor = HuezooColors.AccentYellow,
                    icon = { LightningIcon(color = HuezooColors.AccentYellow) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── 5. Gem breakdown — staggered fade+slide per line item ─────────────
        if (state.gemBreakdown.isNotEmpty()) {
            Spacer(Modifier.height(HuezooSpacing.sm))
            GemBreakdownCard(
                items = state.gemBreakdown,
                totalGems = state.gemsEarned,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(HuezooSpacing.md))

        // ── 5. Buttons ────────────────────────────────────────────────────────
        // "PLAY AGAIN" for both game types when playable; "NO TRIES LEFT" ghost
        // variant for Threshold when exhausted. Daily PLAY AGAIN navigates home.
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HuezooButton(
                    text = when {
                        isDaily -> "PLAY AGAIN"
                        state.canPlayAgain -> "PLAY AGAIN"
                        else -> "NO TRIES LEFT"
                    },
                    onClick = if (state.canPlayAgain || isDaily) onPlayAgain else { {} },
                    variant = if (!isDaily && !state.canPlayAgain) {
                        HuezooButtonVariant.GhostDanger
                    } else {
                        HuezooButtonVariant.Primary
                    },
                    enabled = isDaily || state.canPlayAgain,
                    leadingIcon = if (!isDaily && state.canPlayAgain) {
                        { PlayIcon() }
                    } else {
                        null
                    },
                    modifier = Modifier.weight(1f),
                )
                ShareIconButton(
                    onClick = { onShare(shareText) },
                    icon = shareIcon,
                )
            }
        }

        // Upgrade CTA — shown for Threshold when out of tries and not paid
        if (!isDaily && !state.canPlayAgain && !state.isPaid) {
            Spacer(Modifier.height(HuezooSpacing.xs))
            HuezooButton(
                text = "GO UNLIMITED  →  $2.99",
                onClick = onUpgradeTap,
                variant = HuezooButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Daily: show "Next puzzle in Xh Xm" countdown
        if (isDaily) {
            Spacer(Modifier.height(HuezooSpacing.sm))
            NextPuzzleCountdown(modifier = Modifier.fillMaxWidth())
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), BannerShape)
            .border(1.dp, color.copy(alpha = 0.4f), BannerShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent strip
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(color),
        )
        HuezooLabelSmall(
            text = text,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = HuezooSpacing.sm),
        )
    }
}

@Composable
private fun HeroGems(
    gems: Int,
    color: Color = HuezooColors.AccentCyan,
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
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.08f), Color.Transparent),
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Text(
                text = "+${gems.formatWithCommas()}",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 96.sp,
                    lineHeight = 96.sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = color,
                textAlign = TextAlign.Center,
            )
            HuezooLabelSmall(
                text = "GEMS EARNED",
                color = color.copy(alpha = 0.7f),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
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

/**
 * Gem breakdown card — each line item slides in and fades up staggered.
 * Base delay = 500ms (after hero count-up settles), then +120ms per item.
 */
@Composable
private fun GemBreakdownCard(
    items: List<GemAward>,
    totalGems: Int,
    modifier: Modifier = Modifier,
) {
    // One Animatable (0→1) per item — allocated once and never re-created
    val itemAlphas = remember(items.size) { List(items.size) { Animatable(0f) } }
    val itemSlides = remember(items.size) { List(items.size) { Animatable(16f) } }

    LaunchedEffect(items) {
        items.indices.forEach { i ->
            launch {
                delay(500L + i * 120L)
                launch { itemAlphas[i].animateTo(1f, tween(300)) }
                launch { itemSlides[i].animateTo(0f, tween(300, easing = EaseOutCubic)) }
            }
        }
    }

    Box(
        modifier = modifier
            .background(HuezooColors.SurfaceL1, CardShape)
            .rimLight(cornerRadius = 16.dp)
            .padding(HuezooSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HuezooLabelSmall(
                    text = "GEM BREAKDOWN",
                    color = HuezooColors.TextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                )
                HuezooLabelSmall(
                    text = "GEMS",
                    color = HuezooColors.TextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HuezooColors.SurfaceL3),
            )

            // Staggered line items
            items.forEachIndexed { i, award ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = itemAlphas[i].value
                            translationY = itemSlides[i].value
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HuezooBodyMedium(
                        text = award.label,
                        color = HuezooColors.TextPrimary,
                    )
                    HuezooBodyMedium(
                        text = "+${award.amount}",
                        color = LocalPlayerAccentColor.current,
                    )
                }
            }

            // Total row — visible after all items have appeared
            val totalAlpha = remember { Animatable(0f) }
            LaunchedEffect(items) {
                delay(500L + items.size * 120L + 200L)
                totalAlpha.animateTo(1f, tween(300))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HuezooColors.SurfaceL3)
                    .graphicsLayer { alpha = totalAlpha.value },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = totalAlpha.value },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HuezooLabelSmall(
                    text = "TOTAL",
                    color = HuezooColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                )
                HuezooLabelSmall(
                    text = "+$totalGems",
                    color = LocalPlayerAccentColor.current,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
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
        // Shelf — stationary magenta ledge the face presses into
        Box(
            modifier = Modifier
                .size(ShareButtonSize)
                .offset(y = ShareButtonShelfHeight)
                .clip(ShareButtonShape)
                .background(HuezooColors.ShelfMagenta),
        )
        // Face — slides down on press, springs back on release
        Box(
            modifier = Modifier
                .size(ShareButtonSize)
                .graphicsLayer { translationY = pressProgress * shelfPx }
                .border(1.dp, HuezooColors.AccentMagenta, ShareButtonShape)
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
    val iconColor = LocalPlayerAccentColor.current.onColor
    androidx.compose.foundation.Canvas(modifier = modifier.size(16.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, iconColor)
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

// ── Confetti ──────────────────────────────────────────────────────────────────

private const val CONFETTI_COUNT = 117 // ~90 × 1.3
private const val CONFETTI_DURATION_MS = 4500
private const val CONFETTI_GRAVITY = 0.55f // normalized units per second² (downward)

private enum class ConfettiShape { Hexagon, Circle, Triangle, Capsule, Rect }

private data class ConfettiParticle(
    val x0: Float, // launch x [0..1 fraction of canvas width]
    val y0: Float, // launch y [0..1 fraction — starts in upper third]
    val vx: Float, // horizontal velocity [fraction/s]
    val vy: Float, // initial vertical velocity — negative = upward
    val wobbleAmp: Float, // horizontal sine-wobble amplitude [fraction]
    val wobbleFreq: Float, // wobble frequency [rad/s]
    val wobblePhase: Float, // wobble phase offset [rad]
    val color: Color,
    val rotation0: Float,
    val rotVel: Float,
    val sizeDp: Float,
    val shape: ConfettiShape,
)

@Composable
private fun ConfettiEffect(
    identityColor: Color,
    modifier: Modifier = Modifier,
) {
    val elapsed = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        elapsed.animateTo(
            targetValue = CONFETTI_DURATION_MS / 1000f,
            animationSpec = tween(durationMillis = CONFETTI_DURATION_MS, easing = LinearEasing),
        )
    }

    val particles = remember(identityColor) {
        val rng = Random(seed = 13)
        val colors = listOf(
            identityColor,
            Color.White,
            HuezooColors.AccentCyan,
            HuezooColors.AccentYellow,
            HuezooColors.AccentMagenta,
        )
        val shapes = ConfettiShape.entries
        List(CONFETTI_COUNT) {
            ConfettiParticle(
                // Spread launch x across the full width; cluster more toward center
                x0 = 0.1f + rng.nextFloat() * 0.8f,
                // Launch from upper portion of the screen (0.05 – 0.3)
                y0 = 0.05f + rng.nextFloat() * 0.25f,
                // Gentle horizontal drift
                vx = (rng.nextFloat() - 0.5f) * 0.25f,
                // Negative = shoots upward; then gravity arcs it back down
                vy = -(rng.nextFloat() * 0.45f + 0.15f),
                // Subtle side-to-side wobble for a drifting leaf feel
                wobbleAmp = 0.02f + rng.nextFloat() * 0.04f,
                wobbleFreq = 1.5f + rng.nextFloat() * 2.5f,
                wobblePhase = rng.nextFloat() * (2f * PI.toFloat()),
                color = colors[it % colors.size],
                rotation0 = rng.nextFloat() * 360f,
                rotVel = (rng.nextFloat() - 0.5f) * 240f,
                // Every 5th particle is ~2× larger for visual variety
                sizeDp = if (it % 5 == 0) 18f + rng.nextFloat() * 10f else 7f + rng.nextFloat() * 8f,
                shape = shapes[it % shapes.size],
            )
        }
    }

    val t = elapsed.value
    val durationS = CONFETTI_DURATION_MS / 1000f
    Canvas(modifier = modifier) {
        particles.forEach { p ->
            // Physics: projectile arc + horizontal wobble
            val xFrac = p.x0 + p.vx * t + p.wobbleAmp * sin(p.wobbleFreq * t + p.wobblePhase)
            val yFrac = p.y0 + p.vy * t + 0.5f * CONFETTI_GRAVITY * t * t
            val x = xFrac * size.width
            val y = yFrac * size.height
            // Fade out gently over the last 30% of lifetime
            val lifeProgress = (t / durationS).coerceIn(0f, 1f)
            val alpha = (1f - ((lifeProgress - 0.7f) / 0.3f).coerceIn(0f, 1f)).coerceIn(0f, 1f)
            if (y < size.height * 1.1f && alpha > 0f) {
                val s = p.sizeDp.dp.toPx()
                val c = p.color.copy(alpha = alpha)
                withTransform({
                    translate(x, y)
                    rotate(p.rotation0 + p.rotVel * t)
                }) {
                    when (p.shape) {
                        ConfettiShape.Rect -> drawRect(
                            color = c,
                            topLeft = Offset(-s / 2f, -s * 0.28f),
                            size = Size(s, s * 0.56f),
                        )
                        ConfettiShape.Circle -> drawCircle(
                            color = c,
                            radius = s * 0.38f,
                        )
                        ConfettiShape.Triangle -> {
                            val path = Path().apply {
                                moveTo(0f, -s * 0.48f)
                                lineTo(s * 0.42f, s * 0.24f)
                                lineTo(-s * 0.42f, s * 0.24f)
                                close()
                            }
                            drawPath(path, c)
                        }
                        ConfettiShape.Hexagon -> {
                            val path = Path().apply {
                                for (i in 0..5) {
                                    val angle = (PI / 3.0 * i - PI / 6.0).toFloat()
                                    val px = cos(angle) * s * 0.44f
                                    val py = sin(angle) * s * 0.44f
                                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                                }
                                close()
                            }
                            drawPath(path, c)
                        }
                        ConfettiShape.Capsule -> drawRoundRect(
                            color = c,
                            topLeft = Offset(-s * 0.18f, -s * 0.52f),
                            size = Size(s * 0.36f, s * 1.04f),
                            cornerRadius = CornerRadius(s * 0.18f),
                        )
                    }
                }
            }
        }
    }
}

// ── Daily countdown ───────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
@Composable
private fun NextPuzzleCountdown(modifier: Modifier = Modifier) {
    val text by produceState(initialValue = "") {
        while (true) {
            val now = Clock.System.now()
            val tz = TimeZone.currentSystemDefault()
            val today = now.toLocalDateTime(tz).date
            val nextMidnight = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
            val remaining = nextMidnight - now
            val totalSeconds = remaining.inWholeSeconds.coerceAtLeast(0)
            value = if (totalSeconds <= 0) {
                ""
            } else {
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                if (hours > 0) "Next puzzle in ${hours}h ${minutes}m" else "Next puzzle in ${minutes}m"
            }
            delay(60_000L)
        }
    }
    if (text.isNotEmpty()) {
        HuezooBodyMedium(
            text = text,
            color = HuezooColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = modifier,
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
                roundsSurvived = 4,
                isNewPersonalBest = false,
                personalBestDeltaE = 1.2f,
                gemsEarned = 18,
                gemBreakdown = listOf(
                    xyz.ksharma.huezoo.navigation.GemAward("Correct taps", 8),
                    xyz.ksharma.huezoo.navigation.GemAward("Milestone bonuses", 10),
                ),
            ),
            onPlayAgain = {},
            onShare = {},
            onUpgradeTap = {},
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
                roundsSurvived = 11,
                isNewPersonalBest = true,
                personalBestDeltaE = 0.8f,
                gemsEarned = 57,
                gemBreakdown = listOf(
                    xyz.ksharma.huezoo.navigation.GemAward("Correct taps", 22),
                    xyz.ksharma.huezoo.navigation.GemAward("Milestone bonuses", 35),
                ),
            ),
            onPlayAgain = {},
            onShare = {},
            onUpgradeTap = {},
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
                roundsSurvived = 4,
                isNewPersonalBest = false,
                personalBestDeltaE = null,
                gemsEarned = 23,
                gemBreakdown = listOf(
                    xyz.ksharma.huezoo.navigation.GemAward("Correct rounds ×4", 20),
                    xyz.ksharma.huezoo.navigation.GemAward("Participation", 3),
                ),
            ),
            onPlayAgain = {},
            onShare = {},
            onUpgradeTap = {},
        )
    }
}
