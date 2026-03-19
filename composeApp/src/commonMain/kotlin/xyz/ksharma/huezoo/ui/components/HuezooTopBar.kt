package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.ParallelogramBack
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val TopBarContentHeight = 64.dp
private val BottomBorderHeight = 4.dp
private val BackButtonWidth = 72.dp
private val BackButtonHeight = 40.dp
private val BackShadowOffsetX = 4.dp
private val BackShadowOffsetY = 4.dp
private val BackChevronStroke = 5.dp // ~1.3× original 4dp
private val BackChevronSize = 24.dp // ~1.3× original 18dp
private const val FROST_ALPHA_TOP = 0.95f
private const val FROST_ALPHA_BOTTOM = 0.82f
private const val BACK_PRESS_SCALE = 0.88f
private const val BACK_SHADOW_ALPHA = 0.30f
private val EzLetterSpacing = (-1.3).sp

/**
 * Glassmorphic fixed top bar used on every screen.
 *
 * Layout:
 * ```
 * ┌────────────────────────────────────────┐
 * │  [status bar inset]                    │
 * │  [◁ back? | HUEZ]       [💎 amount?] │  ← 64dp
 * ├────────────────────────────────────────┤  ← 4dp SurfaceL1 border
 * ```
 *
 * Back button rule: when [onBackClick] is provided the wordmark is hidden — the back
 * button owns the left slot entirely. On the home / root screen omit [onBackClick] and
 * the wordmark is shown instead.
 *
 * Background: vertical frost gradient ([Background] 95% → 82%).
 *
 * NEVER use Material3 TopAppBar — use this instead.
 */
@Composable
fun HuezooTopBar(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    currencyAmount: Int? = null,
    gemIcon: androidx.compose.ui.graphics.painter.Painter? = null,
) {
    val frostBrush = Brush.verticalGradient(
        colors = listOf(
            HuezooColors.Background.copy(alpha = FROST_ALPHA_TOP),
            HuezooColors.Background.copy(alpha = FROST_ALPHA_BOTTOM),
        ),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(brush = frostBrush)
                drawRect(
                    color = HuezooColors.SurfaceL1,
                    topLeft = Offset(0f, size.height - BottomBorderHeight.toPx()),
                    size = Size(size.width, BottomBorderHeight.toPx()),
                )
            },
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopBarContentHeight)
                .padding(horizontal = HuezooSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Left: back button XOR wordmark ────────────────────────────────
            if (onBackClick != null) {
                TopBarBackButton(onClick = onBackClick)
            } else {
                HuezooWordmark()
            }

            // ── Right: currency pill ─────────────────────────────────────────
            if (currencyAmount != null && gemIcon != null) {
                CurrencyPill(amount = currencyAmount, icon = gemIcon)
            }
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

/**
 * "HUEZ" brand wordmark in Bebas Neue italic.
 * EZ pair has tighter letter spacing so the letters lean into each other.
 */
@Composable
private fun HuezooWordmark(modifier: Modifier = Modifier) {
    val wordmark = buildAnnotatedString {
        append("HU")
        withStyle(SpanStyle(letterSpacing = EzLetterSpacing)) {
            append("EZ")
        }
    }
    Text(
        text = wordmark,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontStyle = FontStyle.Italic,
        ),
        color = HuezooColors.AccentCyan,
        modifier = modifier,
    )
}

/**
 * Parallelogram back button for the top bar.
 *
 * Visual stack (back to front):
 * 1. Cyan [AccentCyan] parallelogram shadow, offset (+4, +4) dp — visible only on
 *    the right and bottom edges (neo-brutalist press-depth effect)
 * 2. [SurfaceL3] parallelogram fill (the actual button face)
 * 3. Thick `<` chevron drawn via Canvas
 *
 * Press: spring scale-down to [BACK_PRESS_SCALE] — shadow and button scale together.
 *
 * Only shown when the screen has a back destination. When shown, the wordmark is
 * hidden — this component owns the left slot of [HuezooTopBar].
 */
@Composable
private fun TopBarBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) BACK_PRESS_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "backScale",
    )

    Box(
        modifier = modifier
            .size(width = BackButtonWidth, height = BackButtonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shapedShadow(
                shape = ParallelogramBack,
                color = HuezooColors.AccentCyan.copy(alpha = BACK_SHADOW_ALPHA),
                offsetX = BackShadowOffsetX,
                offsetY = BackShadowOffsetY,
            )
            .clip(ParallelogramBack)
            .background(HuezooColors.SurfaceL3)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Button,
                onClickLabel = "Back",
            ),
        contentAlignment = Alignment.Center,
    ) {
        BackChevron()
    }
}

/**
 * Thick `<` chevron drawn via [drawIntoCanvas]. Two lines meeting at a left-pointing
 * vertex, with round stroke caps and join for a friendly chunky feel.
 */
@Composable
private fun BackChevron(modifier: Modifier = Modifier) {
    val strokeColor = HuezooColors.AccentCyan
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(BackChevronSize),
    ) {
        val strokePx = BackChevronStroke.toPx()
        val w = size.width
        val h = size.height
        val tipX = w * 0.20f
        val tipY = h * 0.50f
        val armX = w * 0.75f
        val topY = h * 0.12f
        val botY = h * 0.88f

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                isAntiAlias = true
                style = PaintingStyle.Stroke
                strokeWidth = strokePx
                strokeCap = StrokeCap.Round
                strokeJoin = StrokeJoin.Round
                color = strokeColor
            }
            canvas.drawLine(Offset(tipX, tipY), Offset(armX, topY), paint)
            canvas.drawLine(Offset(tipX, tipY), Offset(armX, botY), paint)
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun HuezooTopBarHomePreview() {
    HuezooPreviewTheme {
        HuezooTopBar(
            currencyAmount = 1250,
            gemIcon = previewGemPainter(),
        )
    }
}

@PreviewComponent
@Composable
private fun HuezooTopBarBackPreview() {
    HuezooPreviewTheme {
        HuezooTopBar(
            onBackClick = {},
            currencyAmount = 1250,
            gemIcon = previewGemPainter(),
        )
    }
}

@PreviewComponent
@Composable
private fun HuezooTopBarBackNoCurrencyPreview() {
    HuezooPreviewTheme {
        HuezooTopBar(
            onBackClick = {},
        )
    }
}

// ── Placeholder painter — swap for painterResource once drawables exist ───────

@Composable
private fun previewGemPainter(): androidx.compose.ui.graphics.painter.Painter =
    androidx.compose.ui.graphics.vector.rememberVectorPainter(
        image = androidx.compose.ui.graphics.vector.ImageVector.Builder(
            defaultWidth = 18.dp,
            defaultHeight = 18.dp,
            viewportWidth = 18f,
            viewportHeight = 18f,
        ).addPath(
            pathData = listOf(
                androidx.compose.ui.graphics.vector.PathNode.MoveTo(9f, 1f),
                androidx.compose.ui.graphics.vector.PathNode.LineTo(3f, 7f),
                androidx.compose.ui.graphics.vector.PathNode.LineTo(9f, 17f),
                androidx.compose.ui.graphics.vector.PathNode.LineTo(15f, 7f),
                androidx.compose.ui.graphics.vector.PathNode.Close,
            ),
            fill = androidx.compose.ui.graphics.SolidColor(HuezooColors.AccentCyan),
        ).build(),
    )
