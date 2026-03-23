package xyz.ksharma.huezoo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration

/**
 * Creates a [NavigationState] that survives configuration changes and process death.
 * Uses polymorphic serialization for multiplatform support (iOS).
 *
 * @param startRoute The initial route shown on app start.
 * @param topLevelRoutes All top-level routes (add more for bottom-nav tabs).
 * @param serializationConfig Serialization configuration for NavKey persistence.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
    serializationConfig: SavedStateConfiguration,
): NavigationState {
    val topLevelRoute = remember { mutableStateOf(startRoute) }
    val backStacks = topLevelRoutes.associateWith { key ->
        rememberNavBackStack(serializationConfig, key)
    }
    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * Holds and manages the navigation back stack(s).
 *
 * Designed for single-stack navigation (no bottom-nav tabs). To add tabs:
 * 1. Add tab routes to `topLevelRoutes` in [rememberNavigationState].
 * 2. Call [goTo] with the tab route — it switches stacks automatically.
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }

    /** Navigate to a route. Top-level routes switch stacks; all others push onto the current stack. */
    fun goTo(route: NavKey) {
        if (route in backStacks.keys) {
            topLevelRoute = route
        } else {
            backStacks[topLevelRoute]?.add(route)
        }
    }

    /** Navigate back. At a top-level route, switches to the start route. */
    fun pop() {
        val currentStack = backStacks[topLevelRoute] ?: return
        val currentRoute = currentStack.last()
        if (currentRoute == topLevelRoute) {
            topLevelRoute = startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    /** Clear the back stack and navigate to [route] — the user cannot navigate back. */
    fun resetRoot(route: NavKey) {
        val currentStack = backStacks[topLevelRoute]
        currentStack?.clear()
        currentStack?.add(route)
    }

    /** Replace the current screen with [route]. Equivalent to popUpTo(current, inclusive = true). */
    fun replaceCurrent(route: NavKey) {
        backStacks[topLevelRoute]?.removeLastOrNull()
        goTo(route)
    }
}

/**
 * Converts this [NavigationState] into a decorated entry list suitable for [NavDisplay].
 *
 * Applies [rememberSaveableStateHolderNavEntryDecorator] so that `rememberSaveable` state
 * (scroll position, text input, etc.) is preserved when the user navigates back.
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            entryProvider = entryProvider,
        )
    }
    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}
