package xyz.ksharma.huezoo.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooTheme

/**
 * Wraps preview content in [HuezooTheme] with a 16 dp padded dark background,
 * matching the app's runtime visual context.
 */
@Composable
fun HuezooPreviewTheme(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    HuezooTheme {
        Box(
            modifier = modifier
                .background(HuezooColors.Background)
                .padding(16.dp),
        ) {
            content()
        }
    }
}
