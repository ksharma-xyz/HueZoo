package xyz.ksharma.huezoo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey

/**
 * Typed navigator for Huezoo. Wraps [NavigationState] and exposes
 * named navigation actions instead of raw route pushes.
 */
class HuezooNavigator(val state: NavigationState) {

    fun goTo(route: NavKey) = state.goTo(route)

    fun pop() = state.pop()

    fun resetRoot(route: NavKey) = state.resetRoot(route)

    fun replaceCurrent(route: NavKey) = state.replaceCurrent(route)

    /**
     * Navigate to the result screen, replacing the current game screen so the
     * back stack becomes [Home, Result] rather than [Home, Game, Result].
     */
    fun navigateToResult(gameId: String) = state.replaceCurrent(Result(gameId))
}

@Composable
fun rememberHuezooNavigator(state: NavigationState): HuezooNavigator =
    remember(state) { HuezooNavigator(state) }
