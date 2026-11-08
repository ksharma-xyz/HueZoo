package xyz.ksharma.huezoo.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window width classes. Decided from the window's width in dp (read it from `BoxWithConstraints`),
 * never from the device type, so iPad, iPhone Duo, resizable windows and landscape phones all use
 * the same code.
 */
enum class WindowClass {
    /** Width below 600dp: the phone layout. */
    Compact,

    /** 600dp up to 840dp: one column capped at [MaxContentWidth]; game content scales up. */
    Medium,

    /** 840dp and wider: multi-pane layouts inside [ExpandedMaxWidth]. */
    Expanded,
}

private val MEDIUM_MIN_WIDTH = 600.dp
private val EXPANDED_MIN_WIDTH = 840.dp

/** Widest the page gets on [WindowClass.Expanded]. Panes sit inside it, centred. */
val ExpandedMaxWidth = 1200.dp

/** Width of the HUD / controls pane next to the game on Expanded windows. */
val SupportingPaneWidth = 360.dp

/**
 * Page width of the supporting-pane game screens (Threshold, Daily) on Expanded windows:
 * HUD pane (360dp) + [PaneGap] + the largest swatch (560dp) + the 16dp side insets. Capping the
 * page here keeps the top bar's edges on the panes' outer edges with no leftover side space.
 */
val GamePageWidth = 976.dp

/** Gap between two panes on [WindowClass.Expanded]. */
val PaneGap = 24.dp

/** Classifies a window by its width. */
fun windowClassFor(width: Dp): WindowClass = when {
    width >= EXPANDED_MIN_WIDTH -> WindowClass.Expanded
    width >= MEDIUM_MIN_WIDTH -> WindowClass.Medium
    else -> WindowClass.Compact
}

/**
 * Caps a screen's page at [ExpandedMaxWidth], filling narrower windows. Place it with
 * `Alignment.TopCenter`. The top bar goes inside this column so its edges are the page edges, and
 * panes use `HuezooSpacing.md` side padding so their outer edges line up with the top bar's
 * buttons.
 */
fun Modifier.expandedMaxWidth(): Modifier = widthIn(max = ExpandedMaxWidth).fillMaxWidth()

/**
 * The page column of a game screen: [MaxContentWidth] on Compact and Medium windows,
 * [ExpandedMaxWidth] on Expanded ones, centred and filling the height. Put the top bar and the
 * screen content inside it.
 */
@Composable
fun PageColumn(
    modifier: Modifier = Modifier,
    expandedWidth: Dp = ExpandedMaxWidth,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = if (windowClassFor(maxWidth) == WindowClass.Expanded) {
            Modifier.widthIn(max = expandedWidth).fillMaxWidth()
        } else {
            Modifier.contentMaxWidth()
        }
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxHeight().then(width),
            content = content,
        )
    }
}
