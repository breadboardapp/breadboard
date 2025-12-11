package moe.apex.rule34.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import moe.apex.rule34.prefs
import moe.apex.rule34.util.BaseHeading
import moe.apex.rule34.util.DISABLED_OPACITY
import moe.apex.rule34.util.ExpressiveTagEntryContainer
import moe.apex.rule34.util.LargeTitleBar
import moe.apex.rule34.util.LargeVerticalSpacer
import moe.apex.rule34.util.ListItemPosition
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.MediumLargeVerticalSpacer
import moe.apex.rule34.util.SMALL_SPACER
import moe.apex.rule34.util.MEDIUM_SPACER
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.Summary
import moe.apex.rule34.util.TINY_SPACER
import moe.apex.rule34.viewmodel.BreadboardViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedTagsScreen(navController: NavHostController, viewModel: BreadboardViewModel) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val scope = rememberCoroutineScope()
    val userPreferencesRepository = LocalContext.current.prefs
    val prefs = LocalPreferences.current
    val blockedTags = prefs.manuallyBlockedTags.reversed()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val aiTags = remember { ImageSource.entries.mapTo(mutableSetOf()) { it.imageBoard.aiTagName }.toList() }

    if (showAddDialog) {
        var content by remember { mutableStateOf("") }
        AlertDialog(
            title = { Text("Add blocked tags") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp)) {
                    PreferenceTextBox(
                        value = content,
                        label = "Tags",
                        autoCorrectEnabled = true
                    ) {
                        content = it.lowercase()
                    }
                    Summary(
                        modifier = Modifier.padding(start = TINY_SPACER.dp),
                        text = "Separate multiple tags with a space."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newBlocks = content
                            .trim()
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .filterNot { it in aiTags }
                        scope.launch {
                            for (tag in newBlocks) {
                                userPreferencesRepository.addToSet(
                                    PreferenceKeys.MANUALLY_BLOCKED_TAGS,
                                    tag
                                )
                            }
                        }
                        showAddDialog = false
                        viewModel.setRecommendationsProvider(null)
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            onDismissRequest = { showAddDialog = false },
        )
    }

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "Blocked tags",
                scrollBehavior = scrollBehavior,
                navController = navController
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, "Add blocked tag")
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(start = MEDIUM_SPACER.dp, end = MEDIUM_SPACER.dp, top = SMALL_LARGE_SPACER.dp, bottom = 88.dp), // FAB height + 16dp vertical padding
        ) {
            item {
                Summary(
                    modifier = Modifier.padding(horizontal = TINY_SPACER.dp),
                    text = "Images with any of these tags will not appear in search results or recommendations. However, they will still show in your Favourites.",
                )
                LargeVerticalSpacer()
            }
            if (!prefs.excludeAi && blockedTags.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(DISABLED_OPACITY),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                        Text(
                            text = "No tags blocked. Search with caution.",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@LazyColumn
            }
            if (prefs.excludeAi) {
                item {
                    BaseHeading(
                        modifier = Modifier.padding(start = SMALL_SPACER.dp, bottom = 6.dp),
                        text = "Automatically blocked"
                    )
                }

                itemsIndexed(aiTags) { index, tag ->
                    ExpressiveTagEntryContainer(
                        modifier = Modifier.animateItem(),
                        label = tag,
                        position = when (index) {
                            0 -> ListItemPosition.TOP
                            aiTags.size - 1 -> ListItemPosition.BOTTOM
                            else -> ListItemPosition.MIDDLE // Not used at the time of writing but if more AI tags appear in the future when it would be useful
                        }
                    )
                }
                if (prefs.manuallyBlockedTags.isNotEmpty()) {
                    item { MediumLargeVerticalSpacer() }
                    item {
                        BaseHeading(
                            modifier = Modifier
                                .padding(start = SMALL_SPACER.dp, bottom = 6.dp)
                                .animateItem(),
                            text = "Blocked by you"
                        )
                    }
                }
            }

            itemsIndexed(blockedTags, key = { index, tag -> tag }) { index, tag ->
                ExpressiveTagEntryContainer(
                    modifier = Modifier.animateItem(),
                    label = tag,
                    position = when {
                        prefs.manuallyBlockedTags.size == 1 -> ListItemPosition.SINGLE_ELEMENT
                        index == 0 -> ListItemPosition.TOP
                        index == prefs.manuallyBlockedTags.size - 1 -> ListItemPosition.BOTTOM
                        else -> ListItemPosition.MIDDLE
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    userPreferencesRepository.removeFromSet(
                                        PreferenceKeys.MANUALLY_BLOCKED_TAGS,
                                        tag
                                    )
                                }
                                viewModel.setRecommendationsProvider(null)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Unblock tag",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }
}
