package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.ParallelogramCard
import xyz.ksharma.huezoo.ui.theme.darken
import xyz.ksharma.huezoo.ui.theme.rimLight
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val DialogShelfOffset = 7.dp
private const val ENTRANCE_SCALE_FROM = 0.82f
private const val SHADOW_ALPHA = 0.85f
private val SkewPad = 26.dp
private val BorderWidth = 2.dp

/**
 * The Huezoo take on an alert dialog — a raised parallelogram card on a neo-brutalist shelf
 * with an accent border, spring entrance, rim-light, and up to two [HuezooButton]s.
 *
 * Prefer this over `androidx.compose.material3.AlertDialog` anywhere the app needs a modal
 * confirm/announce so the surface matches the design system.
 *
 * @param accentColor Colour of the border, title, and (via the button) the primary CTA.
 * @param confirmText Label for the primary action button.
 * @param onConfirm   Invoked when the primary button is tapped.
 * @param dismissText Optional secondary (ghost) button label — omit for a single-action dialog.
 * @param onDismissRequest Back-press / scrim tap. Also used by the secondary button when present.
 */
@Composable
fun HuezooAlertDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = HuezooColors.AccentPurple,
    dismissText: String? = null,
    confirmVariant: HuezooButtonVariant = HuezooButtonVariant.Primary,
    dismissOnConfirm: Boolean = true,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val scale = remember { Animatable(ENTRANCE_SCALE_FROM) }
        val alpha = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            launch {
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 260f))
            }
            launch { alpha.animateTo(1f, tween(160)) }
        }

        Box(
            modifier = modifier
                .padding(horizontal = HuezooSpacing.lg)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .padding(bottom = DialogShelfOffset)
                // Raised: a hard accent-tinted ledge under the card + solid dark drop
                .shapedShadow(
                    ParallelogramCard,
                    HuezooColors.SurfaceL0.copy(alpha = SHADOW_ALPHA),
                    DialogShelfOffset,
                    DialogShelfOffset,
                )
                .shapedShadow(
                    ParallelogramCard,
                    accentColor.darken(0.45f),
                    DialogShelfOffset - 2.dp,
                    DialogShelfOffset - 2.dp,
                )
                .background(HuezooColors.SurfaceL2, ParallelogramCard)
                .border(BorderWidth, accentColor, ParallelogramCard)
                .clip(ParallelogramCard)
                .rimLight(cornerRadius = 20.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = SkewPad, vertical = HuezooSpacing.lg),
            ) {
                HuezooTitleLarge(
                    text = title,
                    color = accentColor,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(HuezooSpacing.sm))
                HuezooBodyMedium(
                    text = message,
                    color = HuezooColors.TextSecondary,
                )
                Spacer(Modifier.height(HuezooSpacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
                ) {
                    if (dismissText != null) {
                        HuezooButton(
                            text = dismissText,
                            onClick = onDismissRequest,
                            variant = HuezooButtonVariant.Ghost,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    HuezooButton(
                        text = confirmText,
                        onClick = {
                            if (dismissOnConfirm) onDismissRequest()
                            onConfirm()
                        },
                        variant = confirmVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun HuezooAlertDialogPreview() {
    HuezooPreviewTheme {
        val accent = HuezooColors.AccentPurple
        // Preview the card body directly — Dialog windows don't render in previews.
        Box(
            modifier = Modifier
                .padding(DialogShelfOffset)
                .shapedShadow(
                    ParallelogramCard,
                    HuezooColors.SurfaceL0.copy(alpha = SHADOW_ALPHA),
                    DialogShelfOffset,
                    DialogShelfOffset,
                )
                .shapedShadow(
                    ParallelogramCard,
                    accent.darken(0.45f),
                    DialogShelfOffset - 2.dp,
                    DialogShelfOffset - 2.dp,
                )
                .background(HuezooColors.SurfaceL2, ParallelogramCard)
                .border(BorderWidth, accent, ParallelogramCard)
                .clip(ParallelogramCard)
                .rimLight(cornerRadius = 20.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = SkewPad, vertical = HuezooSpacing.lg)) {
                HuezooTitleLarge(
                    text = "LEAVE SESSION?",
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(HuezooSpacing.sm))
                HuezooBodyMedium(
                    text = "Progress isn't saved — a session only counts when all 5 rounds are complete.",
                    color = HuezooColors.TextSecondary,
                    textAlign = TextAlign.Start,
                )
                Spacer(Modifier.height(HuezooSpacing.lg))
                Row(horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
                    HuezooButton(
                        text = "STAY",
                        onClick = {},
                        variant = HuezooButtonVariant.Ghost,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(HuezooSpacing.sm))
                    HuezooButton(
                        text = "LEAVE",
                        onClick = {},
                        variant = HuezooButtonVariant.GhostDanger,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
