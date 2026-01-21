package moe.apex.rule34.preferences

import android.os.Build
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
import moe.apex.rule34.util.TitleSummary
import moe.apex.rule34.util.showToast


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalScreen(navController: NavHostController) {
    val context = LocalContext.current
    val preferencesRepository = context.prefs
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
                    for (experiment in Experiment.entries) {
                        item {
                            SwitchPref(
                                title = experiment.label,
                                summary = experiment.description,
                                checked = experiment.isEnabled(),
                                // Immersive carousel uses a blur modifier which requires Android 12+ to work
                                enabled = experiment != Experiment.IMMERSIVE_UI_EFFECTS || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                            ) {
                                scope.launch {
                                    if (it) {
                                        preferencesRepository.addToSet(PreferenceKeys.ENABLED_EXPERIMENTS, experiment)
                                    } else {
                                        preferencesRepository.removeFromSet(PreferenceKeys.ENABLED_EXPERIMENTS, experiment)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                ExpressiveGroup("Debug") {
                    item {
                        TitleSummary(
                            title = "Reset age verification",
                            summary = "This will not disable any currently enabled age-gated " +
                                      "features, but you will need to re-verify if you manually " +
                                      "disable and enable them again."
                        ) {
                            scope.launch {
                                preferencesRepository.updatePref(PreferenceKeys.HAS_VERIFIED_AGE, false)
                            }.invokeOnCompletion {
                                showToast(context, "Done")
                            }
                        }
                    }
                }
            }
        }
    }
}
