package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.LocalPlayerShelfColor
import xyz.ksharma.huezoo.ui.theme.SquircleMedium

private val IconButtonSize = 48.dp
private val IconButtonShelf = 4.dp
private val IconSize = 22.dp

enum class HuezooIconButtonVariant {
    /** Red — close / cancel / dismiss */
    Dismiss,

    /** Green — submit / correct / done */
    Confirm,

    /** Dark — back navigation on non-game screens */
    Back,

    /** Cyan — info / help */
    Info,
}

private data class IconButtonColors(val bg: Color, val shelf: Color)

private fun iconButtonColors(
    variant: HuezooIconButtonVariant,
    levelAccentColor: Color = HuezooColors.AccentCyan,
    levelShelfColor: Color = HuezooColors.ShelfCyan,
): IconButtonColors = when (variant) {
    HuezooIconButtonVariant.Dismiss -> IconButtonColors(
        bg = HuezooColors.ActionDismiss,
        shelf = HuezooColors.ShelfDismiss,
    )
    HuezooIconButtonVariant.Confirm -> IconButtonColors(
        bg = HuezooColors.ActionConfirm,
        shelf = HuezooColors.ShelfConfirm,
    )
    HuezooIconButtonVariant.Back -> IconButtonColors(
        bg = HuezooColors.SurfaceL2,
        shelf = HuezooColors.SurfaceL1,
    )
    HuezooIconButtonVariant.Info -> IconButtonColors(
        bg = levelAccentColor,
        shelf = levelShelfColor,
    )
}

/**
 * Squircle icon button with bottom shelf — the game's primary navigation and
 * action button. Replaces Material back arrows and icon buttons in all game screens.
 *
 * - [Dismiss] (red X) → close, cancel, exit game screen
 * - [Confirm] (green ✓) → submit, correct, done
 * - [Back] (dark ‹) → back navigation on non-game screens
 * - [Info] (cyan ℹ) → help, info overlay
 *
 * NEVER use Material3 `IconButton` + `Icons.AutoMirrored.Default.ArrowBack` in game screens.
 */
@Composable
fun HuezooIconButton(
    variant: HuezooIconButtonVariant,
    icon: Painter,
    onClick: () -> Unit,
    contentDescription: String,
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
        label = "iconBtnPress",
    )

    val playerAccent = LocalPlayerAccentColor.current
    val playerShelf = LocalPlayerShelfColor.current
    val colors = iconButtonColors(variant, playerAccent, playerShelf)
    val resolvedBg = if (enabled) colors.bg else HuezooColors.SurfaceL3
    val resolvedShelf = if (enabled) colors.shelf else HuezooColors.SurfaceL2
    val shelfPx = with(LocalDensity.current) { IconButtonShelf.toPx() }

    Box(modifier = modifier.size(IconButtonSize).padding(bottom = IconButtonShelf)) {
        // Shelf
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 0.dp, y = IconButtonShelf)
                .background(resolvedShelf, SquircleMedium),
        )

        // Face
        Box(
            modifier = Modifier
                .size(IconButtonSize - IconButtonShelf)
                .graphicsLayer {
                    translationX = 0f
                    translationY = pressProgress * shelfPx
                }
                .background(resolvedBg, SquircleMedium)
                .clip(SquircleMedium)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                    role = Role.Button,
                    onClickLabel = contentDescription,
                ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(IconSize),
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────
// Note: use painterResource(Res.drawable.ic_close / ic_check / ic_left / ic_info)
// when the vector drawables are wired up in composeResources/drawable/.

@PreviewComponent
@Composable
private fun HuezooIconButtonPreview() {
    // Placeholder icon painter — a white circle — until vector XMLs are added
    val placeholder = androidx.compose.ui.graphics.vector.rememberVectorPainter(
        image = androidx.compose.ui.graphics.vector.ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = listOf(
                androidx.compose.ui.graphics.vector.PathNode.RelativeMoveTo(12f, 2f),
                androidx.compose.ui.graphics.vector.PathNode.ArcTo(
                    10f,
                    10f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    11.999f,
                    22f,
                ),
                androidx.compose.ui.graphics.vector.PathNode.ArcTo(
                    10f,
                    10f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    12f,
                    2f,
                ),
                androidx.compose.ui.graphics.vector.PathNode.Close,
            ),
            fill = androidx.compose.ui.graphics.SolidColor(Color.White),
        ).build(),
    )
    HuezooPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HuezooIconButton(
                variant = HuezooIconButtonVariant.Dismiss,
                icon = placeholder,
                onClick = {},
                contentDescription = "Close",
            )
            HuezooIconButton(
                variant = HuezooIconButtonVariant.Confirm,
                icon = placeholder,
                onClick = {},
                contentDescription = "Confirm",
            )
            HuezooIconButton(
                variant = HuezooIconButtonVariant.Back,
                icon = placeholder,
                onClick = {},
                contentDescription = "Back",
            )
            HuezooIconButton(
                variant = HuezooIconButtonVariant.Info,
                icon = placeholder,
                onClick = {},
                contentDescription = "Info",
            )
        }
    }
}
