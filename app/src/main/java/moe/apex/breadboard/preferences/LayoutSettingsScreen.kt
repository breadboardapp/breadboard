package moe.apex.breadboard.preferences

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import moe.apex.breadboard.prefs
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.LazyExpressiveGroup
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.MEDIUM_SPACER

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSettingsScreen(navController: NavHostController) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val scope = rememberCoroutineScope()
    val preferencesRepository = LocalContext.current.prefs
    val currentSettings = LocalPreferences.current

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "Behaviour and layout",
                scrollBehavior = scrollBehavior,
                navController = navController
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(MEDIUM_SPACER.dp),
        ) {
            LazyExpressiveGroup(
                title = "Behaviour",
                desiredTopPadding = null
            ) {
                item {
                    EnumPref(
                        title = "Start page",
                        summary = currentSettings.defaultStartDestination.label,
                        enumItems = StartDestination.entries,
                        selectedItem = currentSettings.defaultStartDestination,
                    ) {
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.DEFAULT_START_DESTINATION,
                                it
                            )
                        }
                    }
                }
                /* It's an enum so EnumPref would be the "correct" implementation,
                   but I think this is a better UX as there are currently only two options. */
                item {
                    SwitchPref(
                        checked = currentSettings.defaultBrowseTab == BrowseTab.FOLLOWING,
                        title = "Prefer Following tab",
                        summary = "By default, open the Following tab instead of the For You " +
                                "tab on the Browse page.",
                    ) { following ->
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.DEFAULT_BROWSE_TAB,
                                if (following) BrowseTab.FOLLOWING else BrowseTab.FOR_YOU
                            )
                        }
                    }
                }
                item {
                    EnumPref(
                        title = "Hide app content",
                        summary = currentSettings.flagSecureMode.label,
                        enumItems = FlagSecureMode.entries,
                        selectedItem = currentSettings.flagSecureMode
                    ) {
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.FLAG_SECURE_MODE,
                                it
                            )
                        }
                    }
                }
                item {
                    EnumPref(
                        title = "Data saver",
                        summary = currentSettings.dataSaver.label,
                        enumItems = DataSaver.entries,
                        infoText = "When data saver is enabled, images will load in a " +
                                "lower resolution by default.\n\n" +
                                "Downloads will always be in the maximum resolution " +
                                "regardless of this setting.",
                        selectedItem = currentSettings.dataSaver,
                        onSelection = {
                            scope.launch {
                                preferencesRepository.updatePref(
                                    PreferenceKeys.DATA_SAVER,
                                    it
                                )
                            }
                        }
                    )
                }
            }

            LazyExpressiveGroup("Layout") {
                item {
                    ReorderablePref(
                        title = "Reorder image actions",
                        dialogTitle = "Image actions",
                        summary = "Customise the order of actions in the image viewer. The top " +
                                "action will be displayed separately in its own dedicated button.",
                        items = currentSettings.imageViewerActions
                    ) {
                        scope.launch {
                            preferencesRepository.updateEnumList(
                                PreferenceKeys.IMAGE_VIEWER_ACTION_ORDER,
                                it
                            )
                        }
                    }
                }
                item {
                    SwitchPref(
                        checked = currentSettings.useStaggeredGrid,
                        title = "Staggered grid",
                        summary = "Use a staggered grid for images rather than a uniform grid."
                    ) {
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.USE_STAGGERED_GRID,
                                it
                            )
                        }
                    }
                }
                item {
                    SwitchPref(
                        checked = currentSettings.unifiedInfoSheet,
                        title = "Classic info sheet",
                        summary = "Display all art information in a single scrollable " +
                                "list rather than organising it into tabs."
                    ) {
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.UNIFIED_INFO_SHEET,
                                it
                            )
                        }
                    }
                }
            }
        }
    }
}
