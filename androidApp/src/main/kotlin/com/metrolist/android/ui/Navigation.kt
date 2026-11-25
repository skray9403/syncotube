package com.metrolist.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.filter_library
import com.metrolist.shared.generated.resources.home
import com.metrolist.shared.generated.resources.menu_history
import com.metrolist.shared.generated.resources.mysterious_fourth_tab
import com.metrolist.shared.generated.resources.search
import com.metrolist.shared.generated.resources.settings
import com.metrolist.shared.generated.resources.statistics
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.haptics.HapticEvent
import com.metrolist.shared.ui.haptics.rememberMetrolistHaptics
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.ui.navigation.AdaptiveLayout
import com.metrolist.shared.ui.navigation.AdaptiveNavigationItem
import com.metrolist.shared.ui.navigation.AdaptiveNavigationScaffold
import com.metrolist.shared.ui.navigation.LocalUseDesktopUi
import com.metrolist.shared.ui.navigation.NavigationState
import com.metrolist.shared.ui.navigation.Navigator
import com.metrolist.shared.ui.navigation.Screen
import com.metrolist.shared.ui.navigation.isNavigationBar
import com.metrolist.shared.ui.navigation.visibleSidebarScreens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val Screen.labelRes: StringResource?
    get() =
        when (this) {
            Screen.Home -> Res.string.home
            Screen.Library -> Res.string.filter_library
            Screen.Search -> Res.string.search
            Screen.EasterEgg -> Res.string.mysterious_fourth_tab
            Screen.Settings -> Res.string.settings
            Screen.History -> Res.string.menu_history
            Screen.Statistics -> Res.string.statistics
            else -> null
        }

val Screen.icon: ImageVector?
    get() =
        when (this) {
            Screen.Home -> MetrolistIcons.Outlined.Home
            Screen.Library -> MetrolistIcons.Outlined.LibraryMusic
            Screen.Search -> MetrolistIcons.Outlined.Search
            Screen.Settings -> MetrolistIcons.Outlined.Settings
            Screen.History -> MetrolistIcons.Outlined.History
            Screen.Statistics -> MetrolistIcons.Outlined.SsidChart
            else -> null
        }

val Screen.iconFilled: ImageVector?
    get() =
        when (this) {
            Screen.Home -> MetrolistIcons.Filled.Home
            Screen.Library -> MetrolistIcons.Filled.LibraryMusic
            Screen.Search -> MetrolistIcons.Filled.Search
            Screen.EasterEgg -> MetrolistIcons.Filled.QuestionMark
            Screen.Settings -> MetrolistIcons.Filled.Settings
            Screen.History -> MetrolistIcons.Filled.History
            Screen.Statistics -> MetrolistIcons.Filled.SsidChart
            else -> null
        }

@Composable
fun MetrolistNavigationScaffold(
    navigator: Navigator,
    navigationState: NavigationState,
    adaptiveLayout: AdaptiveLayout,
    navigationSuiteType: NavigationSuiteType,
    navigationVisible: Boolean,
    containerColor: Color,
    @Suppress("UNUSED_PARAMETER") onSearchLongClick: () -> Unit = {},
    floatingNavigation: Boolean = false,
    onFloatingNavigationOccupiedHeightChanged: (Dp) -> Unit = {},
    modifier: Modifier = Modifier,
    selectedItemFocusRequester: FocusRequester = FocusRequester.Default,
    bottomExitFocusRequester: FocusRequester = FocusRequester.Default,
    @Suppress("UNUSED_PARAMETER") contentFocusRequester: FocusRequester = FocusRequester.Default,
    onScrollToTop: (Screen) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val useDesktopUi = LocalUseDesktopUi.current
    val useFloatingNavigation = floatingNavigation && !useDesktopUi
    val screens =
        if (useDesktopUi) {
            (visibleSidebarScreens(Settings.shared.sidebarNavItems) + Screen.Search + Screen.Settings).distinct()
        } else if (useFloatingNavigation) {
            floatingNavigationItems()
        } else {
            visibleNavigationItems()
        }
    val selectedScreen =
        (navigationState.currentRoute.takeIf { it in screens } ?: navigationState.topLevelRoute)
            .takeIf { it in screens } ?: screens.first()
    val items =
        screens.mapNotNull { screen ->
            val label = screen.labelRes?.let { stringResource(it) } ?: return@mapNotNull null
            val icon = screen.icon ?: screen.iconFilled ?: return@mapNotNull null
            AdaptiveNavigationItem(
                key = screen,
                label = label,
                icon = icon,
                selectedIcon = screen.iconFilled ?: icon,
            )
        }
    val hapticManager = rememberMetrolistHaptics()
    val usesNavigationRail = !navigationSuiteType.isNavigationBar

    AdaptiveNavigationScaffold(
        layout = adaptiveLayout,
        items = items,
        selectedKey = selectedScreen,
        onSelect = { screen ->
            hapticManager.perform(HapticEvent.PRESS)
            val shouldScrollToTop =
                navigationState.topLevelRoute == screen &&
                    (screen == Screen.Home || screen == Screen.Library)
            navigator.navigate(screen)
            if (shouldScrollToTop) onScrollToTop(screen)
        },
        modifier = modifier.background(containerColor),
        navigationVisible = navigationVisible,
        floatingNavigation = useFloatingNavigation,
        onFloatingNavigationOccupiedHeightChanged = onFloatingNavigationOccupiedHeightChanged,
        floatingSeparateKey = Screen.Search.takeIf { it in screens },
        navigationSuiteType = navigationSuiteType,
        navigationSuiteColors =
            NavigationSuiteDefaults.colors(
                navigationBarContainerColor = Color.Transparent,
                navigationRailContainerColor = Color.Transparent,
            ),
        containerColor = Color.Transparent,
        isSidebarExpanded = false,
        isSidebarExpandable = false,
        navigationItemVerticalArrangement = Arrangement.Center,
        itemModifier = { screen ->
            val index = screens.indexOf(screen)
            Modifier
                .focusProperties { canFocus = navigationVisible }
                .then(
                    if (usesNavigationRail && index == screens.lastIndex) {
                        Modifier.focusProperties { down = bottomExitFocusRequester }
                    } else {
                        Modifier
                    },
                ).then(
                    if (usesNavigationRail && selectedScreen == screen) {
                        Modifier.focusRequester(selectedItemFocusRequester)
                    } else {
                        Modifier
                    },
                )
        },
        content = { content() },
    )
}

private fun floatingNavigationItems(): List<Screen> =
    buildList {
        if (Settings.shared.showBottomNavHome) add(Screen.Home)
        if (Settings.shared.showBottomNavLibrary) add(Screen.Library)
        if (Settings.shared.showBottomNavSearch) add(Screen.Search)
        if (isEmpty()) add(Screen.EasterEgg)
    }

private fun visibleNavigationItems(): List<Screen> =
    buildList {
        if (Settings.shared.showBottomNavHome) add(Screen.Home)
        if (Settings.shared.showBottomNavSearch) add(Screen.Search)
        if (Settings.shared.showBottomNavLibrary) add(Screen.Library)
        if (isEmpty()) add(Screen.EasterEgg)
    }
