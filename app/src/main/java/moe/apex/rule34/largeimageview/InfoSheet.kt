package moe.apex.rule34.largeimageview


import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.rounded.ContentCopy
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
import moe.apex.rule34.tag.TagCategory
import moe.apex.rule34.ui.theme.prefTitle
import moe.apex.rule34.util.BasicExpressiveContainer
import moe.apex.rule34.util.BasicExpressiveGroup
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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InfoSheet(navController: NavController, image: Image, onDismissRequest: () -> Unit) {
    if (image.metadata == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state: SheetState? = null
    val clip = LocalClipboard.current
    var previousSheetValue by remember { mutableStateOf(SheetValue.Hidden) }

    fun hideAndThen(block: () -> Unit = { }) {
        scope.launch {
            state?.hide()
        }.invokeOnCompletion {
            onDismissRequest()
            block()
        }
    }

    fun chipClick(tag: String) {
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

    fun chipLongClick(tag: String) {
        scope.launch {
            copyText(context, clip, tag)
        }
    }

    // We want to bypass the partially expanded state when closing but not when opening.
    state = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.PartiallyExpanded) {
                if (previousSheetValue == SheetValue.Expanded) {
                    hideAndThen()
                    return@rememberModalBottomSheetState false
                } else {
                    previousSheetValue = newValue
                    return@rememberModalBottomSheetState true
                }
            } else {
                previousSheetValue = newValue
                return@rememberModalBottomSheetState true
            }
        }
    )

    TitledModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = state,
        title = "About this image"
    ) {
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
                                onClick = { launchInWebBrowser(context, it) }, // Breadboard can handle yande.re direct image links. We'll forcibly use the browser here to prevent that here.
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
                                        navController.navigate(ImageView(image.imageSource, it))
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
                        onChipClick = ::chipClick,
                        onChipLongClick = ::chipLongClick
                    )
                }
            }
            /* Artists are stored in their own field rather than in groupedTags.
               If the artist tags are somehow also in groupedTags, we don't want to show them again. */
            image.metadata.groupedTags.filter { it.category != TagCategory.ARTIST }.map {
                item {
                    TagsContainer(
                        category = it.category,
                        tags = it.tags,
                        onChipClick = ::chipClick,
                        onChipLongClick = ::chipLongClick
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
