package xyz.ksharma.huezoo.ui.components

import androidx.compose.runtime.Composable
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewScreen

/** TOP 1% — gold arc gauge almost full, gold neon perimeter traces the highlighted row. */
@PreviewScreen
@Composable
private fun DeltaEWorldRankTop1Preview() {
    HuezooPreviewTheme {
        DeltaEWorldRankSheet(
            deltaE = 0.3f,
            onDismiss = {},
        )
    }
}

/** TOP 20% — green arc gauge, green tier row highlighted. */
@PreviewScreen
@Composable
private fun DeltaEWorldRankTop20Preview() {
    HuezooPreviewTheme {
        DeltaEWorldRankSheet(
            deltaE = 1.8f,
            onDismiss = {},
        )
    }
}

/** TOP 40% — cyan, mid-range player. */
@PreviewScreen
@Composable
private fun DeltaEWorldRankTop40Preview() {
    HuezooPreviewTheme {
        DeltaEWorldRankSheet(
            deltaE = 2.5f,
            onDismiss = {},
        )
    }
}

/** Unranked — no arc gauge, shows UNRANKED banner, full tier ladder with no highlight. */
@PreviewScreen
@Composable
private fun DeltaEWorldRankUnrankedPreview() {
    HuezooPreviewTheme {
        DeltaEWorldRankSheet(
            deltaE = null,
            onDismiss = {},
        )
    }
}
