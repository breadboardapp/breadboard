package moe.apex.breadboard.preferences

import android.os.Build
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
import moe.apex.breadboard.util.Summary
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.TitleSummary
import moe.apex.breadboard.util.saveIgnoreListWithTimestamp
import moe.apex.breadboard.util.showToast
import moe.apex.breadboard.viewmodel.getGlobalViewModel
import kotlin.collections.emptySet


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel = getGlobalViewModel()
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
            contentPadding = PaddingValues(MEDIUM_SPACER.dp)
        ) {
            item {
                Summary(
                    modifier = Modifier.padding(horizontal = TINY_SPACER.dp),
                    text = "These features are incomplete and might have bugs. " +
                           "Future updates may enable an experimental feature by default " +
                           "(at which point it will be removed from this page), or they may " +
                           "remove the feature entirely.\n\n" +
                           "Please report any issues you find with these features on GitHub.",
                )
            }

            LazyExpressiveGroup {
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

            LazyExpressiveGroup("Debug") {
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

                item {
                    TitleSummary(
                        title = "Clear internal ignore list",
                        summary = "Clear Breadboard's internal ignored tag list. " +
                                  "This will trigger a slightly longer refresh of your " +
                                  "recommendations on your next visit."
                    ) {
                        viewModel.resetProviders()
                        scope.launch {
                            saveIgnoreListWithTimestamp(
                                context = context,
                                data = emptySet(),
                                timestamp = 0L
                            )
                        }.invokeOnCompletion {
                            showToast(context, "Done")
                        }
                    }
                }
            }
        }
    }
}
