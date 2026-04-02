package xyz.ksharma.huezoo.ui.upgrade

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooDisplayLarge
import xyz.ksharma.huezoo.ui.components.HuezooDisplayMedium
import xyz.ksharma.huezoo.ui.components.HuezooHeadlineMedium
import xyz.ksharma.huezoo.ui.components.HuezooLabelLarge
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.PriceButton
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewScreen
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.ParallelogramBack
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val HeroSize = 200.dp
private const val RING_ALPHA_PEAK = 0.45f
private const val RING_PERIOD_MS = 2200
private const val RING_STAGGER_MS = 733
private const val LARGE_FONT_SCALE_THRESHOLD = 1.3f

private data class Feature(
    val symbol: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
)

private val FeatureList = listOf(
    Feature("∞", "UNLIMITED", "Threshold tries", HuezooColors.AccentCyan),
    Feature("◈", "DAILY", "Always free", HuezooColors.GameDaily),
    Feature("◉", "LEADERBOARD", "Global rank", HuezooColors.GameThreshold),
    Feature("+", "ALL MODES", "Future included", HuezooColors.AccentYellow),
)

/**
 * Paywall screen — single entry point for all purchase CTAs.
 *
 * Navigation entrypoints:
 *  • Home screen: GET FULL ACCESS CTA below Threshold card (blocked + free)
 *  • Future: result screen upsell after out-of-tries game
 *
 * Always navigate here for upgrade — never inline [PriceButton] elsewhere.
 *
 * TODO: Wire onPurchase to real IAP (Google Play Billing / StoreKit 2).
 *       Fetch price string from store at runtime — currently hardcoded.
 */
@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpgradeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Entrance: slide up + fade in
    val offsetY = remember { Animatable(80f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            offsetY.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 180f),
            )
        }
        launch { alpha.animateTo(1f, tween(400)) }
    }

    AmbientGlowBackground(
        modifier = modifier,
        primaryColor = HuezooColors.PriceGreen,
        secondaryColor = HuezooColors.AccentCyan,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            HuezooTopBar(onBackClick = onBack, currencyAmount = null)

            // Scrollable body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .graphicsLayer {
                        translationY = offsetY.value
                        this.alpha = alpha.value
                    }
                    .padding(horizontal = HuezooSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(HuezooSpacing.md))

                // ── Animated hero ─────────────────────────────────────────────
                Box(
                    modifier = Modifier.size(HeroSize),
                    contentAlignment = Alignment.Center,
                ) {
                    PulsingDiamondRings(color = HuezooColors.PriceGreen)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HuezooDisplayLarge(
                            text = "∞",
                            color = HuezooColors.PriceGreen,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        HuezooLabelSmall(
                            text = "UNLIMITED",
                            color = HuezooColors.PriceGreen,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }

                Spacer(Modifier.height(HuezooSpacing.md))

                // ── Headline ──────────────────────────────────────────────────
                HuezooHeadlineMedium(
                    text = "Train Without Limits",
                    color = HuezooColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(HuezooSpacing.xs))

                HuezooLabelSmall(
                    text = "Free tier: 5 Threshold tries per day.\nFull access removes the cap — play as much as you want.",
                    color = HuezooColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(HuezooSpacing.xxl))

                // ── Feature tiles — adaptive layout ───────────────────────────
                val fontScale = LocalDensity.current.fontScale
                Column(verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
                    if (fontScale > LARGE_FONT_SCALE_THRESHOLD) {
                        FeatureList.forEach { feature ->
                            FeatureRow(
                                symbol = feature.symbol,
                                title = feature.title,
                                subtitle = feature.subtitle,
                                accentColor = feature.accentColor,
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
                        ) {
                            FeatureTile(
                                symbol = "∞",
                                title = "UNLIMITED",
                                subtitle = "Threshold tries",
                                accentColor = HuezooColors.AccentCyan,
                                modifier = Modifier.weight(1f),
                            )
                            FeatureTile(
                                symbol = "◈",
                                title = "DAILY",
                                subtitle = "Always free",
                                accentColor = HuezooColors.GameDaily,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
                        ) {
                            FeatureTile(
                                symbol = "◉",
                                title = "LEADERBOARD",
                                subtitle = "Global rank",
                                accentColor = HuezooColors.GameThreshold,
                                modifier = Modifier.weight(1f),
                            )
                            FeatureTile(
                                symbol = "+",
                                title = "ALL MODES",
                                subtitle = "Future included",
                                accentColor = HuezooColors.AccentYellow,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(HuezooSpacing.xxl))
            }

            // ── Pinned price + purchase CTA ───────────────────────────────────
            UpgradeBottomCta(
                state = state,
                alpha = alpha.value,
                onPurchase = { viewModel.onPurchase() },
                onRestore = { viewModel.onRestorePurchases() },
            )
        }
    }
}

// ── Animated hero ─────────────────────────────────────────────────────────────

/**
 * Three concentric diamond rings that pulse outward with [color], staggered
 * 733 ms apart to create a continuous ripple. Ring alpha fades to 0 as it
 * expands so the motion feels like energy radiating from the center.
 */
@Composable
private fun PulsingDiamondRings(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "diamondRings")

    val p1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RING_PERIOD_MS),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0),
        ),
        label = "ring1",
    )
    val p2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RING_PERIOD_MS),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(RING_STAGGER_MS),
        ),
        label = "ring2",
    )
    val p3 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RING_PERIOD_MS),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(RING_STAGGER_MS * 2),
        ),
        label = "ring3",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val maxRadius = size.minDimension / 2f

        listOf(p1, p2, p3).forEach { progress ->
            val radius = maxRadius * progress
            val ringAlpha = RING_ALPHA_PEAK * (1f - progress)
            val strokePx = (2.5f - progress * 1.5f).coerceAtLeast(0.5f) * density

            val cx = center.x
            val cy = center.y
            val path = Path().apply {
                moveTo(cx, cy - radius)
                lineTo(cx + radius, cy)
                lineTo(cx, cy + radius)
                lineTo(cx - radius, cy)
                close()
            }
            drawPath(
                path = path,
                color = color.copy(alpha = ringAlpha),
                style = Stroke(width = strokePx),
            )
        }
    }
}

// ── Feature tile ──────────────────────────────────────────────────────────────

/**
 * Parallelogram feature tile — same shape language as the top-bar back button
 * and SkewedStatChip. Each tile has a symbol, a bold title, and a subtitle.
 * [accentColor] tints the shadow and symbol to give each tile its own identity.
 */
@Composable
private fun FeatureTile(
    symbol: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shapedShadow(
                shape = ParallelogramBack,
                color = accentColor.copy(alpha = 0.25f),
                offsetX = 4.dp,
                offsetY = 4.dp,
            )
            .clip(ParallelogramBack)
            .background(HuezooColors.SurfaceL2),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HuezooDisplayMedium(
                text = symbol,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
            )
            HuezooLabelLarge(
                text = title,
                color = HuezooColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
            )
            HuezooLabelSmall(
                text = subtitle,
                color = HuezooColors.TextSecondary,
            )
        }
    }
}

// ── Feature row (large font fallback) ────────────────────────────────────────

/**
 * Single-row feature item for large-font layouts — parallelogram badge on the
 * left, title + subtitle text on the right. Matches the shape language of
 * [FeatureTile] without a fixed height so text never clips.
 */
@Composable
private fun FeatureRow(
    symbol: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shapedShadow(
                shape = ParallelogramBack,
                color = accentColor.copy(alpha = 0.25f),
                offsetX = 4.dp,
                offsetY = 4.dp,
            )
            .clip(ParallelogramBack)
            .background(HuezooColors.SurfaceL2)
            .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
    ) {
        // Parallelogram badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(ParallelogramBack)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            HuezooDisplayMedium(
                text = symbol,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Column {
            HuezooLabelLarge(
                text = title,
                color = HuezooColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
            )
            HuezooLabelSmall(
                text = subtitle,
                color = HuezooColors.TextSecondary,
            )
        }
    }
}

// ── Pinned CTA ────────────────────────────────────────────────────────────────

@Composable
private fun UpgradeBottomCta(
    state: UpgradeUiState,
    alpha: Float,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .padding(horizontal = HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Price displayed separately — large and prominent
        HuezooDisplayLarge(
            text = state.priceLabel,
            color = HuezooColors.PriceGreen,
            fontWeight = FontWeight.ExtraBold,
        )

        HuezooLabelSmall(
            text = "ONE-TIME PURCHASE · NO SUBSCRIPTION",
            color = HuezooColors.TextDisabled,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(HuezooSpacing.sm))

        PriceButton(
            price = if (state.isPurchasing) "PURCHASING…" else "UNLOCK FOREVER",
            onClick = onPurchase,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.error != null) {
            Spacer(Modifier.height(HuezooSpacing.xs))
            HuezooLabelSmall(
                text = state.error,
                color = HuezooColors.AccentMagenta,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(HuezooSpacing.sm))

        HuezooLabelSmall(
            text = "Supports indie development ♥",
            color = HuezooColors.TextDisabled,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(HuezooSpacing.xs))

        HuezooButton(
            text = "RESTORE PURCHASES",
            onClick = onRestore,
            variant = HuezooButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(HuezooSpacing.lg))
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@PreviewScreen
@Composable
private fun UpgradeScreenPreview() {
    HuezooPreviewTheme {
        UpgradeScreen(onBack = {})
    }
}

@PreviewScreen
@Composable
private fun UpgradeBottomCtaErrorPreview() {
    HuezooPreviewTheme {
        UpgradeBottomCta(
            state = UpgradeUiState(
                priceLabel = "$2.99",
                error = "Purchase failed. Check your connection and try again.",
            ),
            alpha = 1f,
            onPurchase = {},
            onRestore = {},
        )
    }
}
