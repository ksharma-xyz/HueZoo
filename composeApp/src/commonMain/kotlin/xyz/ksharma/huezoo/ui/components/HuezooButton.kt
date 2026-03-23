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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.platform.haptics.HapticType
import xyz.ksharma.huezoo.platform.haptics.LocalHapticEngine
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.LocalPlayerShelfColor
import xyz.ksharma.huezoo.ui.theme.PillShape
import xyz.ksharma.huezoo.ui.theme.onColor

private val ShelfHeight = 5.dp

enum class HuezooButtonVariant { Primary, Confirm, Danger, Score, Try, Ghost, GhostDanger }

/**
 * Controls how a [HuezooButton] sizes itself horizontally.
 *
 * - [Fill] — fills the available container width. Use for primary CTAs and bottom-of-screen actions.
 * - [Wrap] — wraps to label width. Use for inline or secondary actions placed in a Row.
 *
 * Width is orthogonal to [HuezooButtonVariant] (which controls colour/purpose).
 * The caller still controls the button's position and constraints via [Modifier].
 */
enum class HuezooButtonWidth { Fill, Wrap }

private data class ButtonColors(
    val bg: Color,
    val content: Color,
    val shelf: Color,
    val border: Color = Color.Transparent,
)

private fun buttonColors(
    variant: HuezooButtonVariant,
    levelAccentColor: Color = HuezooColors.AccentCyan,
    levelShelfColor: Color = HuezooColors.ShelfCyan,
): ButtonColors = when (variant) {
    HuezooButtonVariant.Primary -> ButtonColors(
        bg = levelAccentColor,
        content = levelAccentColor.onColor,
        shelf = levelShelfColor,
    )
    HuezooButtonVariant.Confirm -> ButtonColors(
        bg = HuezooColors.ActionConfirm,
        content = HuezooColors.ActionConfirm.onColor, // dark — green is bright (contrast ~8.8:1)
        shelf = HuezooColors.ShelfConfirm,
    )
    HuezooButtonVariant.Danger -> ButtonColors(
        bg = HuezooColors.AccentMagenta,
        content = HuezooColors.AccentMagenta.onColor, // dark — magenta medium-bright (contrast ~5.6:1)
        shelf = HuezooColors.ShelfMagenta,
    )
    HuezooButtonVariant.Score -> ButtonColors(
        bg = HuezooColors.AccentYellow,
        content = HuezooColors.AccentYellow.onColor, // dark — yellow is very bright (contrast ~14:1)
        shelf = HuezooColors.ShelfYellow,
    )
    HuezooButtonVariant.Try -> ButtonColors(
        bg = HuezooColors.ActionTry,
        content = HuezooColors.ActionTry.onColor, // dark — blue passes with dark text (~5.4:1)
        shelf = HuezooColors.ShelfTry,
    )
    HuezooButtonVariant.Ghost -> ButtonColors(
        bg = Color.Transparent,
        content = levelAccentColor,
        shelf = HuezooColors.SurfaceL1,
        border = levelAccentColor,
    )
    HuezooButtonVariant.GhostDanger -> ButtonColors(
        bg = Color.Transparent,
        content = HuezooColors.AccentMagenta,
        shelf = HuezooColors.ShelfMagenta,
        border = HuezooColors.AccentMagenta,
    )
}

/**
 * Candy-style game button for the Huezoo design system.
 *
 * Hard bottom shelf (no diagonal offset) gives a physical "press down" feel.
 * On press the face translates DOWN into the shelf and springs back on release.
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
    width: HuezooButtonWidth = HuezooButtonWidth.Fill,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticEngine.current
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

    val playerAccent = LocalPlayerAccentColor.current
    val playerShelf = LocalPlayerShelfColor.current
    val colors = buttonColors(variant, playerAccent, playerShelf)
    val resolvedBg = if (enabled) colors.bg else HuezooColors.SurfaceL3
    val resolvedContent = if (enabled) colors.content else HuezooColors.TextDisabled
    val resolvedShelf = if (enabled) colors.shelf else HuezooColors.SurfaceL2
    val resolvedBorder = if (enabled) colors.border else Color.Transparent

    val shelfPx = with(LocalDensity.current) { ShelfHeight.toPx() }

    // Outer box reserves bottom space so the shelf doesn't get clipped by the parent
    Box(modifier = modifier.padding(bottom = ShelfHeight)) {
        // Shelf — stationary; face presses into it on touch
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = ShelfHeight)
                .background(resolvedShelf, PillShape),
        )
        // Face — fills or wraps based on width intent; slides down to meet shelf on press
        Box(
            modifier = Modifier
                .then(if (width == HuezooButtonWidth.Fill) Modifier.fillMaxWidth() else Modifier)
                .graphicsLayer { translationY = pressProgress * shelfPx }
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
                    onClick = {
                        haptic.perform(HapticType.ButtonTap)
                        onClick()
                    },
                )
                .padding(horizontal = HuezooSpacing.lg, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(HuezooSpacing.sm))
                }
                HuezooLabelLarge(
                    text = text,
                    color = resolvedContent,
                    fontWeight = FontWeight.ExtraBold,
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
