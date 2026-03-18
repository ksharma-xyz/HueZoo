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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.SquircleButton

enum class HuezooButtonVariant { Primary, Danger, Ghost, Score }

/**
 * Main button for the Huezoo design system.
 *
 * Variants:
 * - [HuezooButtonVariant.Primary] — cyan fill, dark text (main CTA)
 * - [HuezooButtonVariant.Danger]  — magenta fill (destructive / wrong)
 * - [HuezooButtonVariant.Ghost]   — transparent with cyan border (secondary)
 * - [HuezooButtonVariant.Score]   — yellow fill (game achievement)
 *
 * Press animation: scale 1.0 → 0.94 instantly, spring back on release.
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

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 80)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "buttonScale",
    )

    val bgColor: Color
    val contentColor: Color
    val borderColor: Color

    when (variant) {
        HuezooButtonVariant.Primary -> {
            bgColor = HuezooColors.AccentCyan
            contentColor = HuezooColors.Background
            borderColor = Color.Transparent
        }
        HuezooButtonVariant.Danger -> {
            bgColor = HuezooColors.AccentMagenta
            contentColor = HuezooColors.TextPrimary
            borderColor = Color.Transparent
        }
        HuezooButtonVariant.Ghost -> {
            bgColor = Color.Transparent
            contentColor = HuezooColors.AccentCyan
            borderColor = HuezooColors.AccentCyan
        }
        HuezooButtonVariant.Score -> {
            bgColor = HuezooColors.AccentYellow
            contentColor = HuezooColors.Background
            borderColor = Color.Transparent
        }
    }

    val resolvedContentColor = if (enabled) contentColor else HuezooColors.TextDisabled
    val resolvedBgColor = if (enabled) bgColor else HuezooColors.SurfaceL3

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.5.dp, if (enabled) borderColor else HuezooColors.TextDisabled, SquircleButton)
                } else {
                    Modifier
                },
            )
            .background(resolvedBgColor, SquircleButton)
            .clip(SquircleButton)
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
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun HuezooButtonPrimaryPreview() {
    HuezooPreviewTheme {
        HuezooButton(text = "Unlock Forever — \$2", onClick = {})
    }
}

@PreviewComponent
@Composable
private fun HuezooButtonVariantsPreview() {
    HuezooPreviewTheme {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            HuezooButton(text = "Primary", onClick = {}, variant = HuezooButtonVariant.Primary)
            HuezooButton(text = "Watch Ad (+1 try)", onClick = {}, variant = HuezooButtonVariant.Ghost)
            HuezooButton(text = "Danger", onClick = {}, variant = HuezooButtonVariant.Danger)
            HuezooButton(text = "Score", onClick = {}, variant = HuezooButtonVariant.Score)
            HuezooButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
