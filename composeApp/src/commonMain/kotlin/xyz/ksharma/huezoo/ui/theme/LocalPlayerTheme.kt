package xyz.ksharma.huezoo.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * CompositionLocal that carries the player's current level accent color
 * through the entire composition tree.
 *
 * Provided at the root [App] composable by reading the player's gem count
 * and deriving [PlayerLevel.levelColor]. All UI chrome that uses a "primary"
 * accent reads this instead of the hardcoded [HuezooColors.AccentCyan].
 *
 * Default = Rookie cyan — safe for previews and any scope without a provider.
 *
 * **Do NOT use this for game-mechanic colors** (correct/wrong tile borders,
 * ΔE difficulty scale, danger variants). Those are fixed semantic colors.
 */
val LocalPlayerAccentColor = compositionLocalOf<Color> { HuezooColors.AccentCyan }

/**
 * Shelf (3-D shadow) counterpart to [LocalPlayerAccentColor].
 * Each level has a pre-defined darker shelf color in [HuezooColors].
 */
val LocalPlayerShelfColor = compositionLocalOf<Color> { HuezooColors.ShelfCyan }
