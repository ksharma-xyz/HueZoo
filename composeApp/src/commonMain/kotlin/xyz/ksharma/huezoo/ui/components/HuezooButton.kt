package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors

private val ButtonShape = RoundedCornerShape(12.dp)
private val ShadowOffset = 4.dp

// Shadow colors — darker versions of each variant's accent
private val ShadowColorPrimary = Color(0xFF00A3B8)
private val ShadowColorDanger = Color(0xFFB3004E)
private val ShadowColorScore = Color(0xFFCC9900)

enum class HuezooButtonVariant { Primary, Danger, Ghost, Score }

private data class ButtonColors(
    val bg: Color,
    val content: Color,
    val shadow: Color,
    val border: Color,
)

private fun buttonColors(variant: HuezooButtonVariant): ButtonColors = when (variant) {
    HuezooButtonVariant.Primary -> ButtonColors(
        bg = HuezooColors.AccentCyan,
        content = HuezooColors.Background,
        shadow = ShadowColorPrimary,
        border = Color.Transparent,
    )
    HuezooButtonVariant.Danger -> ButtonColors(
        bg = HuezooColors.AccentMagenta,
        content = HuezooColors.TextPrimary,
        shadow = ShadowColorDanger,
        border = Color.Transparent,
    )
    HuezooButtonVariant.Ghost -> ButtonColors(
        bg = Color.Transparent,
        content = HuezooColors.AccentCyan,
        shadow = HuezooColors.SurfaceL1,
        border = HuezooColors.AccentCyan,
    )
    HuezooButtonVariant.Score -> ButtonColors(
        bg = HuezooColors.AccentYellow,
        content = HuezooColors.Background,
        shadow = ShadowColorScore,
        border = Color.Transparent,
    )
}

/**
 * Neo-brutalist button for the Huezoo design system.
 *
 * Each variant has a hard flat colored shadow at [ShadowOffset] below/right —
 * no blur, just a solid rectangle. On press the button layer translates INTO
 * the shadow; on release it springs back.
 *
 * Variants:
 * - [HuezooButtonVariant.Primary] — cyan fill, dark text (main CTA)
 * - [HuezooButtonVariant.Danger]  — magenta fill (destructive / wrong)
 * - [HuezooButtonVariant.Ghost]   — transparent with cyan border (secondary)
 * - [HuezooButtonVariant.Score]   — yellow fill (game achievement)
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

    val resolvedBgColor = if (enabled) colors.bg else HuezooColors.SurfaceL3
    val resolvedContentColor = if (enabled) colors.content else HuezooColors.TextDisabled
    val resolvedShadowColor = if (enabled) colors.shadow else HuezooColors.SurfaceL2
    val resolvedBorderColor = if (enabled) colors.border else Color.Transparent

    val shadowOffsetPx = with(LocalDensity.current) { ShadowOffset.toPx() }

    // Outer box reserves space for the shadow so layout doesn't shift
    Box(modifier = modifier.padding(end = ShadowOffset, bottom = ShadowOffset)) {
        // Hard shadow layer — stays fixed while button layer moves
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = ShadowOffset, y = ShadowOffset)
                .background(resolvedShadowColor, ButtonShape),
        )

        // Button layer — translates toward shadow on press
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = pressProgress * shadowOffsetPx
                    translationY = pressProgress * shadowOffsetPx
                }
                .then(
                    if (resolvedBorderColor != Color.Transparent) {
                        Modifier.border(1.5.dp, resolvedBorderColor, ButtonShape)
                    } else {
                        Modifier
                    },
                )
                .background(resolvedBgColor, ButtonShape)
                .clip(ButtonShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = resolvedContentColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
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
            HuezooButton(text = "Watch Ad (+1 try)", onClick = {}, variant = HuezooButtonVariant.Ghost)
            HuezooButton(text = "Wrong — Try Again", onClick = {}, variant = HuezooButtonVariant.Danger)
            HuezooButton(text = "Unlock Forever — \$2", onClick = {}, variant = HuezooButtonVariant.Score)
            HuezooButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
