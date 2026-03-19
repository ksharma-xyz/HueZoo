package xyz.ksharma.huezoo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors

/**
 * Game-styled [ModalBottomSheet] wrapper.
 *
 * - Surface: [HuezooColors.SurfaceL2]
 * - Top corners: 32 dp rounded
 * - Custom handle bar
 *
 * Pass [sheetState] if you need to control dismiss programmatically:
 * ```
 * val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
 * HuezooBottomSheet(onDismissRequest = { ... }, sheetState = sheetState) { ... }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuezooBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = HuezooColors.SurfaceL2,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { HuezooSheetHandle() },
        content = content,
    )
}

@Composable
private fun HuezooSheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(HuezooColors.TextDisabled, CircleShape),
        )
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@PreviewComponent
@Composable
private fun HuezooBottomSheetPreview() {
    HuezooPreviewTheme {
        // Static preview of the sheet content — ModalBottomSheet cannot be
        // previewed directly; show the interior layout instead.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HuezooColors.SurfaceL2, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        ) {
            HuezooSheetHandle()
            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                HuezooTitleMedium(
                    text = "Unlock Forever — \$2",
                )
                Spacer(Modifier.height(12.dp))
                HuezooButton(text = "Unlock Forever — \$2", onClick = {
                }, variant = HuezooButtonVariant.Primary, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                HuezooButton(text = "Watch Ad (+1 try)", onClick = {
                }, variant = HuezooButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
