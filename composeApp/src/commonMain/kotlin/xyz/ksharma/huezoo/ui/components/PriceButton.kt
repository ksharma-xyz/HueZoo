package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.PillShape

private val PriceButtonHeight = 56.dp
private val PriceButtonShelf = 6.dp

private const val GLOSS_ALPHA = 0.14f
private const val GLOSS_WIDTH_FRACTION = 0.30f
private const val GLOSS_HEIGHT_FRACTION = 0.30f
private const val GLOSS_CENTER_X_FRACTION = 0.20f
private const val GLOSS_CENTER_Y_FRACTION = 0.28f

/**
 * Full-width pill-shaped purchase CTA.
 *
 * Shows [price] prominently (e.g. "$2.99", "$24.99") on a bright green face with
 * a dark green bottom shelf. Intended for Paywall sheet and purchase confirmations.
 *
 * NEVER use `HuezooButton` or `Material3.Button` for purchase CTAs — use this instead.
 */
@Composable
fun PriceButton(
    price: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1f else 0f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 80)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "pricePress",
    )

    val faceColor = if (enabled) HuezooColors.PriceGreen else HuezooColors.SurfaceL3
    val shelfColor = if (enabled) HuezooColors.ShelfPrice else HuezooColors.SurfaceL2
    val textColor = if (enabled) Color.White else HuezooColors.TextDisabled

    val shelfPx = with(LocalDensity.current) { PriceButtonShelf.toPx() }

    Box(modifier = modifier.fillMaxWidth().padding(bottom = PriceButtonShelf)) {
        // Shelf
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 0.dp, y = PriceButtonShelf)
                .background(shelfColor, PillShape),
        )

        // Face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PriceButtonHeight)
                .graphicsLayer {
                    translationX = 0f
                    translationY = pressProgress * shelfPx
                }
                .background(faceColor, PillShape)
                .clip(PillShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width * GLOSS_WIDTH_FRACTION
                val h = size.height * GLOSS_HEIGHT_FRACTION
                val cx = size.width * GLOSS_CENTER_X_FRACTION
                val cy = size.height * GLOSS_CENTER_Y_FRACTION
                drawOval(
                    color = Color.White.copy(alpha = GLOSS_ALPHA),
                    topLeft = Offset(cx - w / 2f, cy - h / 2f),
                    size = Size(w, h),
                )
            }
            Text(
                text = price,
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun PriceButtonPreview() {
    HuezooPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PriceButton(price = "$2.99", onClick = {})
            PriceButton(price = "$24.99", onClick = {})
            PriceButton(price = "$9.99", onClick = {}, enabled = false)
        }
    }
}
