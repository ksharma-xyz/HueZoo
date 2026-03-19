package xyz.ksharma.huezoo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.PillShape

private val GemIconSize = 20.dp

/**
 * Read-only pill showing a gem/currency icon and an [amount].
 *
 * Displayed in the top bar on every screen as a persistent currency counter.
 * No border, no press animation — display element only.
 *
 * The [icon] painter should be a cyan/blue gem icon.
 * Pass `painterResource(Res.drawable.ic_gem)` as [icon].
 */
@Composable
fun CurrencyPill(
    amount: Int,
    icon: Painter,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(HuezooColors.SurfaceL2, PillShape)
            .clip(PillShape)
            .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(GemIconSize),
            )
            Spacer(Modifier.width(HuezooSpacing.xs))
            HuezooHeadlineSmall(
                text = amount.toString(),
                color = HuezooColors.TextPrimary,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun CurrencyPillPreview() {
    val gemPlaceholder = androidx.compose.ui.graphics.vector.rememberVectorPainter(
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
    HuezooPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
            CurrencyPill(amount = 512, icon = gemPlaceholder)
            CurrencyPill(amount = 1250, icon = gemPlaceholder)
        }
    }
}
