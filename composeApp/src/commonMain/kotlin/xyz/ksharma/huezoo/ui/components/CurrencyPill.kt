package xyz.ksharma.huezoo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.PillShape

private val GemIconSize = 18.dp

/**
 * Read-only pill showing a gem/currency icon and an [amount].
 *
 * Displayed in the Home screen top-right (e.g. Home screen header) as a currency counter.
 * No press animation — this is a display element only.
 *
 * Pass a vector painter from `painterResource(Res.drawable.ic_gem)` as [icon].
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
            .border(HuezooSize.BorderThin, HuezooColors.GemGreen, PillShape)
            .clip(PillShape)
            .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.xs + 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(GemIconSize),
            )
            Spacer(Modifier.width(HuezooSpacing.xs))
            Text(
                text = amount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = HuezooColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun CurrencyPillPreview() {
    // Placeholder icon — replace with painterResource(Res.drawable.ic_gem)
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
            fill = androidx.compose.ui.graphics.SolidColor(HuezooColors.GemGreen),
        ).build(),
    )
    HuezooPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
            CurrencyPill(amount = 512, icon = gemPlaceholder)
            CurrencyPill(amount = 1250, icon = gemPlaceholder)
        }
    }
}
