package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.PillShape

private val ShelfHeight = 5.dp

private const val GLOSS_ALPHA = 0.14f
private const val GLOSS_WIDTH_FRACTION = 0.35f
private const val GLOSS_HEIGHT_FRACTION = 0.30f
private const val GLOSS_CENTER_X_FRACTION = 0.25f
private const val GLOSS_CENTER_Y_FRACTION = 0.28f

enum class HuezooButtonVariant { Primary, Confirm, Danger, Score, Try, Ghost }

private data class ButtonColors(
    val bg: Color,
    val content: Color,
    val shelf: Color,
    val border: Color = Color.Transparent,
)

private fun buttonColors(variant: HuezooButtonVariant): ButtonColors = when (variant) {
    HuezooButtonVariant.Primary -> ButtonColors(
        bg = HuezooColors.AccentCyan,
        content = HuezooColors.Background,
        shelf = HuezooColors.ShelfCyan,
    )
    HuezooButtonVariant.Confirm -> ButtonColors(
        bg = HuezooColors.ActionConfirm,
        content = HuezooColors.TextPrimary,
        shelf = HuezooColors.ShelfConfirm,
    )
    HuezooButtonVariant.Danger -> ButtonColors(
        bg = HuezooColors.AccentMagenta,
        content = HuezooColors.TextPrimary,
        shelf = HuezooColors.ShelfMagenta,
    )
    HuezooButtonVariant.Score -> ButtonColors(
        bg = HuezooColors.AccentYellow,
        content = HuezooColors.Background,
        shelf = HuezooColors.ShelfYellow,
    )
    HuezooButtonVariant.Try -> ButtonColors(
        bg = HuezooColors.ActionTry,
        content = HuezooColors.TextPrimary,
        shelf = HuezooColors.ShelfTry,
    )
    HuezooButtonVariant.Ghost -> ButtonColors(
        bg = Color.Transparent,
        content = HuezooColors.AccentCyan,
        shelf = HuezooColors.SurfaceL1,
        border = HuezooColors.AccentCyan,
    )
}

/**
 * Candy-style game button for the Huezoo design system.
 *
 * Hard bottom shelf (no diagonal offset) gives a physical "press down" feel.
 * A subtle gloss oval sits at the top-left of the face. On press the face
 * translates DOWN into the shelf and springs back on release.
 *
 * Variants:
 * - [HuezooButtonVariant.Primary] — cyan (main CTA)
 * - [HuezooButtonVariant.Confirm] — green (submit / correct)
 * - [HuezooButtonVariant.Danger]  — magenta (destructive / wrong)
 * - [HuezooButtonVariant.Score]   — yellow (achievement)
 * - [HuezooButtonVariant.Try]     — blue (secondary / try)
 * - [HuezooButtonVariant.Ghost]   — transparent + cyan border
 */
@Composable
fun HuezooButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HuezooButtonVariant = HuezooButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
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
        label = "buttonPress",
    )

    val colors = buttonColors(variant)
    val resolvedBg = if (enabled) colors.bg else HuezooColors.SurfaceL3
    val resolvedContent = if (enabled) colors.content else HuezooColors.TextDisabled
    val resolvedShelf = if (enabled) colors.shelf else HuezooColors.SurfaceL2
    val resolvedBorder = if (enabled) colors.border else Color.Transparent

    val shelfPx = with(LocalDensity.current) { ShelfHeight.toPx() }

    // Reserve bottom space for the shelf
    Box(modifier = modifier.padding(bottom = ShelfHeight)) {
        // Shelf — fixed, bottom only
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 0.dp, y = ShelfHeight)
                .background(resolvedShelf, PillShape),
        )

        // Face — slides down on press, springs back on release
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = 0f
                    translationY = pressProgress * shelfPx
                }
                .then(
                    if (resolvedBorder != Color.Transparent) {
                        Modifier.border(HuezooSize.BorderThin, resolvedBorder, PillShape)
                    } else {
                        Modifier
                    },
                )
                .background(resolvedBg, PillShape)
                .clip(PillShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(horizontal = HuezooSpacing.lg, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Gloss overlay — top-left white oval
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(HuezooSpacing.sm))
                }
                Text(
                    text = text,
                    color = resolvedContent,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun HuezooButtonVariantsPreview() {
    HuezooPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HuezooButton(text = "Play Now", onClick = {}, variant = HuezooButtonVariant.Primary)
            HuezooButton(text = "Submit Answer", onClick = {}, variant = HuezooButtonVariant.Confirm)
            HuezooButton(text = "Watch Ad (+1 try)", onClick = {}, variant = HuezooButtonVariant.Ghost)
            HuezooButton(text = "Game Over — Try Again", onClick = {}, variant = HuezooButtonVariant.Danger)
            HuezooButton(text = "Unlock Forever — \$2", onClick = {}, variant = HuezooButtonVariant.Score)
            HuezooButton(text = "Try Different Color", onClick = {}, variant = HuezooButtonVariant.Try)
            HuezooButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
