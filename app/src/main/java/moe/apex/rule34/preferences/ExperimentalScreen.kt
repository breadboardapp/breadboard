package moe.apex.rule34.preferences

import androidx.compose.foundation.layout.Arrangement
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
import moe.apex.rule34.prefs
import moe.apex.rule34.util.ExpressiveGroup
import moe.apex.rule34.util.LARGE_SPACER
import moe.apex.rule34.util.LargeTitleBar
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.MEDIUM_SPACER
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.Summary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalScreen(navController: NavHostController) {
    val preferencesRepository = LocalContext.current.prefs
    val prefs = LocalPreferences.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "Experimental features",
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
            contentPadding = PaddingValues(vertical = MEDIUM_SPACER.dp),
            verticalArrangement = Arrangement.spacedBy(LARGE_SPACER.dp)
        ) {
            item {
                Summary(
                    modifier = Modifier.padding(horizontal = SMALL_LARGE_SPACER.dp),
                    text = "These features are incomplete and might have bugs. " +
                           "Future updates may enable an experimental feature by default " +
                           "(at which point it will be removed from this page), or they may " +
                           "remove the feature entirely.\n\n" +
                           "Please report any issues you find with these features on GitHub.",
                )
            }
            item {
                ExpressiveGroup {
                    item {
                        SwitchPref(
                            title = "Pull-to-refresh in search",
                            summary = "Enable pull-to-refresh in search results.",
                            checked = prefs.searchPullToRefresh,
                        ) {
                            scope.launch {
                                preferencesRepository.updatePref(
                                    PreferenceKeys.SEARCH_PULL_TO_REFRESH,
                                    it
                                )
                            }
                        }
                    }
                    item {
                        SwitchPref(
                            title = "Always animate scroll-to-top",
                            summary = "Enable smooth scrolling on all pages when using the scroll-to-top button.",
                            checked = prefs.alwaysAnimateScroll,
                        ) {
                            scope.launch {
                                preferencesRepository.updatePref(PreferenceKeys.ALWAYS_ANIMATE_SCROLL, it)
                            }
                        }
                    }
                }
            }
        }
    }
}
