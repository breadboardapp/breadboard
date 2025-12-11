package moe.apex.rule34.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.prefs
import moe.apex.rule34.tag.IgnoredTagsHelper
import moe.apex.rule34.util.DISABLED_OPACITY
import moe.apex.rule34.util.ExpressiveTagEntryContainer
import moe.apex.rule34.util.LargeTitleBar
import moe.apex.rule34.util.LargeVerticalSpacer
import moe.apex.rule34.util.ListItemPosition
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.MEDIUM_SPACER
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.Summary
import moe.apex.rule34.util.TINY_SPACER
import moe.apex.rule34.util.saveIgnoreListWithTimestamp
import moe.apex.rule34.util.showToast
import moe.apex.rule34.viewmodel.BreadboardViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgnoredTagsScreen(navController: NavHostController, viewModel: BreadboardViewModel) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val context = LocalContext.current
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val userPreferencesRepository = LocalContext.current.prefs
    val prefs = LocalPreferences.current
    val ignoredTags = prefs.unfollowedTags.reversed()

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "Ignored tags",
                scrollBehavior = scrollBehavior,
                navController = navController,
                additionalActions = {
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    var refreshEnabled by remember { mutableStateOf(true) }
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More"
                            )
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.small,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    enabled = refreshEnabled,
                                    text = { Text("Refresh meta tags") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        refreshEnabled = false
                                        scope.launch {
                                            IgnoredTagsHelper.fetchTagListOnline(
                                                context = context,
                                                onSuccess = {
                                                    saveIgnoreListWithTimestamp(context, it)
                                                    viewModel.setRecommendationsProvider(null)
                                                    withContext(Dispatchers.Main) {
                                                        showToast(context, "Refreshed ${it.size} tags")
                                                    }
                                                },
                                                onFailure = {
                                                    withContext(Dispatchers.Main) {
                                                        showToast(context, "Failed to refresh tags")
                                                    }
                                                }
                                            )
                                        }.invokeOnCompletion {
                                            refreshEnabled = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(start = MEDIUM_SPACER.dp, end = MEDIUM_SPACER.dp, top = SMALL_LARGE_SPACER.dp, bottom = SMALL_LARGE_SPACER.dp),
        ) {
            item {
                Summary(
                    modifier = Modifier.padding(horizontal = TINY_SPACER.dp),
                    text = "Ignoring a frequent tag means that Breadboard will not use it to " +
                           "recommend new content. Ignored tags are not blocked and you may " +
                           "still see content with them, but they will not be used to influence " +
                           "recommendations."
                )
                LargeVerticalSpacer()
            }
            if (ignoredTags.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(DISABLED_OPACITY),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                        Text(
                            text = "No ignored tags.",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@LazyColumn
            }

            itemsIndexed(ignoredTags, key = { _, tag -> tag }) { index, tag ->
                ExpressiveTagEntryContainer(
                    modifier = Modifier.animateItem(),
                    label = tag,
                    position = when {
                        prefs.unfollowedTags.size == 1 -> ListItemPosition.SINGLE_ELEMENT
                        index == 0 -> ListItemPosition.TOP
                        index == prefs.unfollowedTags.size - 1 -> ListItemPosition.BOTTOM
                        else -> ListItemPosition.MIDDLE
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    userPreferencesRepository.removeFromSet(
                                        PreferenceKeys.UNFOLLOWED_TAGS,
                                        tag
                                    )
                                }
                                viewModel.setRecommendationsProvider(null)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Unignore tag",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }
}
