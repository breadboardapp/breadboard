package moe.apex.rule34.largeimageview


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.rule34.DeepLinkActivity
import moe.apex.rule34.MainActivity
import moe.apex.rule34.image.Image
import moe.apex.rule34.navigation.ImageView
import moe.apex.rule34.navigation.Results
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.preferences.PreferenceKeys
import moe.apex.rule34.prefs
import moe.apex.rule34.tag.TagCategory
import moe.apex.rule34.ui.theme.prefTitle
import moe.apex.rule34.util.BasicExpressiveContainer
import moe.apex.rule34.util.BasicExpressiveGroup
import moe.apex.rule34.util.ButtonListItem
import moe.apex.rule34.util.CHIP_SPACING
import moe.apex.rule34.util.ChevronRight
import moe.apex.rule34.util.CombinedClickableFilterChip
import moe.apex.rule34.util.LARGE_SPACER
import moe.apex.rule34.util.ListItemPosition
import moe.apex.rule34.util.MEDIUM_LARGE_SPACER
import moe.apex.rule34.util.MEDIUM_SPACER
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.TitleSummary
import moe.apex.rule34.util.TitledModalBottomSheet
import moe.apex.rule34.util.copyText
import moe.apex.rule34.util.isWebLink
import moe.apex.rule34.util.largerShape
import moe.apex.rule34.util.launchInWebBrowser
import moe.apex.rule34.util.navBarHeight
import moe.apex.rule34.util.openUrl
import moe.apex.rule34.util.pluralise
import moe.apex.rule34.util.showToast


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InfoSheet(navController: NavController, image: Image, onDismissRequest: () -> Unit) {
    /* I don't really like this whole info/options implementation.
       Ideally we'd use AnimatedContent or something and switch between the two in the same sheet.
       However, unfortunately the built-in M3 ModalBottomSheet has an endless list of problems
       that aren't getting fixed and a few of them affect what I wanted to do.
       I'm still not totally happy with this implementation with options being a separate dialog,
       but I think it's the best we're going to get at this time.  */
    if (image.metadata == null) return
    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val clip = LocalClipboard.current
    val preferencesRepository = context.prefs
    val scope = rememberCoroutineScope()

    /* This is sketchy but we need to be able to access the sheet state itself in its own
       confirmValueChange which for some reason we can't normally do in order to disable the
       partially expanded state only when dismissing.  */
    var sheetState: SheetState? by remember { mutableStateOf(null) }
    var selectedTag: String? by remember { mutableStateOf(null) }

    fun hideAndThen(block: () -> Unit = { }) {
        scope.launch {
            sheetState?.hide()
        }.invokeOnCompletion {
            onDismissRequest()
            block()
        }
    }

    fun startTagSearch(tag: String) {
        hideAndThen {
            /* Don't do new searches inside the DeepLinkActivity. We should only
               ever do them inside the main one. */
            if (context is DeepLinkActivity) {
                val intent = createSearchIntent(context, image.imageSource, tag)
                context.startActivity(intent)
            } else {
                navController.navigate(Results(image.imageSource, listOf(tag)))
            }
        }
    }

    // We want to bypass the partially expanded state when closing but not when opening.
    sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    ) { newValue ->
        if (newValue == SheetValue.PartiallyExpanded) {
            if (sheetState?.currentValue == SheetValue.Expanded) {
                hideAndThen()
                return@rememberModalBottomSheetState false
            } else {
                return@rememberModalBottomSheetState true
            }
        } else {
            return@rememberModalBottomSheetState true
        }
    }

    TitledModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState ?: return,
        title = "About this image",
    ) {
        if (selectedTag != null) {
            /* We need to have this dialog inside the sheet otherwise it'll just automatically
               dismiss itself and the sheet because this entire system sucks. */
            val blocked = selectedTag in prefs.blockedTags
            BasicAlertDialog(
                onDismissRequest = { selectedTag = null },
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.extraLarge
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = LARGE_SPACER.dp,
                        end = LARGE_SPACER.dp,
                        top = LARGE_SPACER.dp,
                        bottom = LARGE_SPACER.dp + MEDIUM_SPACER.dp // The chip has 8dp padding so we should really match them but I think looks more balanced.
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SMALL_LARGE_SPACER.dp)
                ) {
                    CombinedClickableFilterChip(
                        label = { Text(selectedTag!!) },
                        warning = blocked,
                        onClick = { },
                        onLongClick = { },
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        ButtonListItem(
                            label = "Search",
                            icon = Icons.Rounded.Search,
                            modifier = Modifier.fillMaxWidth(),
                            position = ListItemPosition.TOP
                        ) {
                            val searchTag = selectedTag!!
                            selectedTag = null
                            startTagSearch(searchTag)
                        }
                        ButtonListItem(
                            label = "Copy to clipboard",
                            icon = Icons.Rounded.ContentCopy,
                            modifier = Modifier.fillMaxWidth(),
                            position = ListItemPosition.MIDDLE
                        ) {
                            scope.launch {
                                copyText(context, clip, selectedTag!!)
                            }
                        }
                        ButtonListItem(
                            label = "${if (blocked) "Unblock" else "Block"} this tag",
                            icon = if (blocked) Icons.Rounded.CheckCircleOutline else Icons.Rounded.Block,
                            modifier = Modifier.fillMaxWidth(),
                            position = ListItemPosition.BOTTOM
                        ) {
                            if (blocked) {
                                scope.launch {
                                    preferencesRepository.removeFromSet(
                                        PreferenceKeys.MANUALLY_BLOCKED_TAGS,
                                        selectedTag!!
                                    )
                                }
                                showToast(context, "Unblocked tag ${selectedTag!!}")
                            } else {
                                scope.launch {
                                    preferencesRepository.addToSet(
                                        PreferenceKeys.MANUALLY_BLOCKED_TAGS,
                                        selectedTag!!
                                    )
                                }
                                showToast(context, "Blocked tag ${selectedTag!!}")
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MEDIUM_SPACER.dp)
                .clip(largerShape),
            verticalArrangement = Arrangement.spacedBy(LARGE_SPACER.dp),
            contentPadding = PaddingValues(bottom = navBarHeight * 2)
        ) {
            item {
                Row {
                    BasicExpressiveContainer(
                        modifier = Modifier.weight(1f),
                        position = ListItemPosition.SINGLE_ELEMENT
                    ) {
                        TitleSummary(
                            title = image.metadata.rating.label,
                            summary = "Rating"
                        )
                    }
                    Spacer(Modifier.width(MEDIUM_LARGE_SPACER.dp))
                    BasicExpressiveContainer(
                        modifier = Modifier.weight(1f),
                        position = ListItemPosition.SINGLE_ELEMENT
                    ) {
                        TitleSummary(
                            title = image.imageSource.label,
                            summary = "Imageboard"
                        )
                    }
                }
            }
            item {
                BasicExpressiveGroup {
                    image.metadata.source?.let {
                        val title = "Source"
                        item {
                            TitleSummary(
                                title = title,
                                summary = it,
                                onClick = if (it.isWebLink()) {
                                    {
                                        openUrl(context, it)
                                    }
                                } else null,
                                trailingIcon = if (it.isWebLink()) {
                                    {
                                        CopyIcon(title) {
                                            scope.launch { copyText(context, clip, it) }
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                    image.metadata.pixivUrl?.let {
                        val title = "Pixiv URL"
                        item {
                            TitleSummary(
                                title = title,
                                summary = it,
                                onClick = if (it.isWebLink()) {
                                    {
                                        openUrl(context, it)
                                    }
                                } else null,
                                trailingIcon = if (it.isWebLink()) {
                                    {
                                        CopyIcon(title) {
                                            scope.launch { copyText(context, clip, it) }
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                    image.highestQualityFormatUrl.let {
                        val title = "File URL"
                        item {
                            TitleSummary(
                                title = title,
                                summary = it,
                                onClick = {
                                    launchInWebBrowser(
                                        context,
                                        it
                                    )
                                }, // Breadboard can handle yande.re direct image links. We'll forcibly use the browser here to prevent that here.
                                trailingIcon = {
                                    CopyIcon(title) {
                                        scope.launch { copyText(context, clip, it) }
                                    }
                                }
                            )
                        }
                    }
                    image.metadata.parentId?.let {
                        item {
                            TitleSummary(
                                title = "View parent image",
                                onClick = {
                                    hideAndThen {
                                        navController.navigate(
                                            ImageView(
                                                image.imageSource,
                                                it
                                            )
                                        )
                                    }
                                },
                                trailingIcon = {
                                    ChevronRight()
                                }
                            )
                        }
                    }
                    if (image.metadata.hasChildren == true) {
                        image.id?.let {
                            item {
                                TitleSummary(
                                    title = "View related images",
                                    onClick = {
                                        hideAndThen {
                                            if (context is DeepLinkActivity) {
                                                val intent = createSearchIntent(
                                                    context,
                                                    image.imageSource,
                                                    "parent:$it"
                                                )
                                                context.startActivity(intent)
                                            } else {
                                                navController.navigate(
                                                    Results(
                                                        image.imageSource,
                                                        listOf("parent:$it")
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        ChevronRight()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            image.metadata.artists.takeIf { it.isNotEmpty() }?.let {
                item {
                    TagsContainer(
                        category = TagCategory.ARTIST,
                        tags = it,
                        onChipClick = { startTagSearch(it) },
                        onChipLongClick = {
                            selectedTag = it
                        }
                    )
                }
            }
            /* Artists are stored in their own field rather than in groupedTags.
               If the artist tags are somehow also in groupedTags,
               we don't want to show them again. */
            image.metadata.groupedTags.filter { it.category != TagCategory.ARTIST }.map {
                item {
                    TagsContainer(
                        category = it.category,
                        tags = it.tags,
                        onChipClick = { startTagSearch(it) },
                        onChipLongClick = {
                            selectedTag = it
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun CopyIcon(itemType: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = "Copy $itemType",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Suppress("DEPRECATION")
@Composable
private fun TagsContainer(
    category: TagCategory,
    tags: List<String>,
    onChipClick: (String) -> Unit,
    onChipLongClick: (String) -> Unit
) {
    val prefs = LocalPreferences.current
    BasicExpressiveContainer(position = ListItemPosition.SINGLE_ELEMENT) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    top = SMALL_LARGE_SPACER.dp,
                    bottom = (SMALL_LARGE_SPACER - 8).dp, // Chips have 8dp vertical padding already
                    start = SMALL_LARGE_SPACER.dp,
                    end = SMALL_LARGE_SPACER.dp
                )
        ) {
            Text(
                text = category.label.pluralise(
                    tags.size,
                    category.pluralisedLabel
                ),
                style = MaterialTheme.typography.prefTitle,
            )
            ContextualFlowRow(
                itemCount = tags.size,
                horizontalArrangement = Arrangement.spacedBy(
                    space = CHIP_SPACING.dp,
                    alignment = Alignment.Start
                )
            ) { index ->
                val tag = tags[index]
                CombinedClickableFilterChip(
                    label = { Text(tag) },
                    warning = tag in prefs.blockedTags,
                    onClick = { onChipClick(tag) },
                    onLongClick = { onChipLongClick(tag) }
                )
            }
        }
    }
}


private fun createSearchIntent(context: Context, imageSource: ImageSource, query: String): Intent {
    return createSearchIntent(context, imageSource, listOf(query))
}


private fun createSearchIntent(context: Context, imageSource: ImageSource, queries: List<String>): Intent {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.putExtra("source", imageSource.name)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.putExtra("query", queries.toTypedArray())
    intent.setComponent(
        ComponentName(
            context,
            MainActivity::class.java
        )
    )
    return intent
}
