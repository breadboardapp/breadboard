package moe.apex.rule34.largeimageview


import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import moe.apex.rule34.util.ButtonListItem
import moe.apex.rule34.util.CHIP_SPACING
import moe.apex.rule34.util.ChevronRight
import moe.apex.rule34.util.CombinedClickableFilterChip
import moe.apex.rule34.util.ExpressiveGroupScope
import moe.apex.rule34.util.HorizontalFloatingToolbarOptionalFab
import moe.apex.rule34.util.LARGE_SPACER
import moe.apex.rule34.util.ListItemPosition
import moe.apex.rule34.util.MEDIUM_LARGE_SPACER
import moe.apex.rule34.util.MEDIUM_SPACER
import moe.apex.rule34.util.LazyExpressiveGroup
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.TINY_SPACER
import moe.apex.rule34.util.TitleSummary
import moe.apex.rule34.util.TitledModalBottomSheet
import moe.apex.rule34.util.bouncyAnimationSpec
import moe.apex.rule34.util.copyText
import moe.apex.rule34.util.isWebLink
import moe.apex.rule34.util.largerShapeCornerSize
import moe.apex.rule34.util.launchInWebBrowser
import moe.apex.rule34.util.navBarHeight
import moe.apex.rule34.util.openUrl
import moe.apex.rule34.util.pluralise
import moe.apex.rule34.util.showToast


private enum class InfoSheetPage {
    SOURCES,
    IMAGEBOARD
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
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

    val unified = prefs.unifiedInfoSheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = !unified)
    var sheetPage by remember { mutableStateOf(InfoSheetPage.SOURCES) }

    fun hideAndThen(block: () -> Unit = { }) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            onDismissRequest()
            block()
        }
    }

    /* The unified sheet should always open in half-expanded state,
       but should never go back to the half-expanded state when closing.  */
    if (unified) {
        LaunchedEffect(sheetState.targetValue) {
            if (sheetState.currentValue == SheetValue.Expanded && sheetState.targetValue == SheetValue.PartiallyExpanded) {
                hideAndThen()
            }
        }
    }

    var selectedTag: String? by remember { mutableStateOf(null) }

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

    val onCopyClick: (String) -> Unit = { scope.launch { copyText(context, clip, it) }}

    TitledModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
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
                        bottom = LARGE_SPACER.dp + MEDIUM_SPACER.dp // The chip has 8dp padding so we should really match them but I think this looks more balanced.
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

        Box(
            modifier = Modifier
                .padding(horizontal = MEDIUM_SPACER.dp)
                .clip(RoundedCornerShape(topStart = largerShapeCornerSize, topEnd = largerShapeCornerSize))
        ) {
            // https://www.crunchyroll.com/series/GP5HJ8E81/
            val onLinkClick = { url: String ->
                openUrl(context, url)
            }
            val onBrowserLinkClick = { url: String -> // Bypass Breadboard's handling of file URLs like for Yande.re
                launchInWebBrowser(context, url)
            }
            val onTagLongClick = { tag: String -> selectedTag = tag }
            val onViewParentClick = { id: String ->
                hideAndThen {
                    navController.navigate(ImageView(image.imageSource, id))
                }
            }

            if (unified) {
                UnifiedInfoContent(
                    image = image,
                    onLinkClick = onLinkClick,
                    onBrowserLinkClick = onBrowserLinkClick,
                    onCopyClick = onCopyClick,
                    onViewParentClick = onViewParentClick,
                    onViewRelatedClick = { startTagSearch("parent:$it") },
                    onTagClick = ::startTagSearch,
                    onTagLongClick = onTagLongClick
                )
            } else {
                AnimatedContent(
                    targetState = sheetPage,
                    contentAlignment = Alignment.Center,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                ) { page ->
                    when (page) {
                        InfoSheetPage.SOURCES -> InfoTabContent(
                            image = image,
                            onLinkClick = onLinkClick,
                            onBrowserLinkClick = onBrowserLinkClick,
                            onCopyClick = onCopyClick,
                            onViewParentClick = onViewParentClick,
                            onViewRelatedClick = { startTagSearch("parent:$it") },
                            onTagClick = ::startTagSearch,
                            onTagLongClick = onTagLongClick
                        )

                        InfoSheetPage.IMAGEBOARD -> ImageboardDataTabContent(
                            image = image,
                            onLinkClick = onLinkClick,
                            onBrowserLinkClick = onBrowserLinkClick,
                            onCopyClick = onCopyClick,
                            onTagClick = ::startTagSearch,
                            onTagLongClick = onTagLongClick
                        )
                    }
                }

                SharedTransitionLayout(modifier = Modifier.align(Alignment.BottomCenter)) {
                    val sharedElementModifier = @Composable { visible: Boolean ->
                        Modifier.sharedElementWithCallerManagedVisibility(
                            sharedContentState = rememberSharedContentState("toolbar"),
                            visible = visible,
                            boundsTransform = { _, _ -> bouncyAnimationSpec() }
                        )
                    }

                    HorizontalFloatingToolbarOptionalFab(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = SMALL_LARGE_SPACER.dp),
                    ) {
                        BottomToolbarButton(
                            activeIndicatorModifier = sharedElementModifier(sheetPage == InfoSheetPage.SOURCES),
                            textButtonModifier = Modifier.renderInSharedTransitionScopeOverlay(),
                            label = "Sources",
                            isActive = sheetPage == InfoSheetPage.SOURCES,
                            onClick = {
                                sheetPage = InfoSheetPage.SOURCES
                            }
                        )

                        BottomToolbarButton(
                            activeIndicatorModifier = sharedElementModifier(sheetPage == InfoSheetPage.IMAGEBOARD),
                            textButtonModifier = Modifier.renderInSharedTransitionScopeOverlay(),
                            label = "Imageboard",
                            isActive = sheetPage == InfoSheetPage.IMAGEBOARD,
                            onClick = {
                                sheetPage = InfoSheetPage.IMAGEBOARD
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun BottomToolbarButton(
    @SuppressLint("ModifierParameter") activeIndicatorModifier: Modifier = Modifier,
    textButtonModifier: Modifier = Modifier,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    )
    Box {
        Box(
            modifier = activeIndicatorModifier
                .matchParentSize()
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
        )
        TextButton(
            modifier = textButtonModifier
                .heightIn(min = LocalMinimumInteractiveComponentSize.current)
                .semantics { this.selected = isActive }, // We're using this button basically as a tab, so we should provide a way to indicate to talkback that it's selected.
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = contentColor
            )
        ) {
            Text(label, modifier = Modifier.padding(horizontal = MEDIUM_SPACER.dp))
        }
    }
}


@Composable
private fun InfoTabContent(
    image: Image,
    onLinkClick: (String) -> Unit,
    onBrowserLinkClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onViewParentClick: (String) -> Unit,
    onViewRelatedClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit
) {
    SplitInfoSheetLazyColumn {
        infoContentItems(
            image = image,
            onLinkClick = onLinkClick,
            onBrowserLinkClick = onBrowserLinkClick,
            onCopyClick = onCopyClick,
            onViewParentClick = onViewParentClick,
            onViewRelatedClick = onViewRelatedClick,
            onTagClick = onTagClick,
            onTagLongClick = onTagLongClick
        )
    }
}


@Composable
private fun ImageboardDataTabContent(
    image: Image,
    onLinkClick: (String) -> Unit,
    onBrowserLinkClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit
) {
    SplitInfoSheetLazyColumn {
        imageboardDataContentItems(
            image = image,
            onLinkClick = onLinkClick,
            onBrowserLinkClick = onBrowserLinkClick,
            onCopyClick = onCopyClick,
            onTagClick = onTagClick,
            onTagLongClick = onTagLongClick
        )
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UnifiedInfoContent(
    image: Image,
    onLinkClick: (String) -> Unit,
    onBrowserLinkClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onViewParentClick: (String) -> Unit,
    onViewRelatedClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            bottom = navBarHeight + MEDIUM_SPACER.dp,
        )
    ) {
        infoContentItems(
            image = image,
            onLinkClick = onLinkClick,
            onBrowserLinkClick = onBrowserLinkClick,
            onCopyClick = onCopyClick,
            onViewParentClick = onViewParentClick,
            onViewRelatedClick = onViewRelatedClick,
            onTagClick = onTagClick,
            onTagLongClick = onTagLongClick,
            unified = true
        )
        imageboardDataContentItems(
            image = image,
            onLinkClick = onLinkClick,
            onBrowserLinkClick = onBrowserLinkClick,
            onCopyClick = onCopyClick,
            onTagClick = onTagClick,
            onTagLongClick = onTagLongClick,
            unified = true
        )
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SplitInfoSheetLazyColumn(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            bottom = navBarHeight + FloatingToolbarDefaults.ContainerSize + 32.dp, // Height of the toolbar + 16dp vertical padding
        )
    ) {
        content()
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.infoContentItems(
    image: Image,
    onLinkClick: (String) -> Unit,
    onBrowserLinkClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onViewParentClick: (String) -> Unit,
    onViewRelatedClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit,
    unified: Boolean = false
) {
    item {
        Row {
            BasicExpressiveContainer(
                modifier = Modifier.weight(1f),
                position = ListItemPosition.SINGLE_ELEMENT
            ) {
                TitleSummary(
                    title = image.metadata!!.rating.label,
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

    LazyExpressiveGroup {
        image.metadata!!.source?.let {
            UrlItem(
                label = "Source",
                url = it,
                onLinkClick = if (it.isWebLink()) { { onLinkClick(it) } } else null,
                onCopyClick = if (it.isWebLink()) { { onCopyClick(it) } } else null
            )
        }

        image.metadata.pixivUrl?.let {
            if (!it.isWebLink()) return@let

            UrlItem(
                label = "Pixiv URL",
                url = it,
                onLinkClick = { onLinkClick(it) },
                onCopyClick = { onCopyClick(it) }
            )
        }

        /* In unified mode, display file URL with the other URLs.
           In split mode, it's displayed on the other tab.  */
        if (unified) {
            image.highestQualityFormatUrl.let {
                UrlItem(
                    label = "File URL",
                    url = it,
                    onLinkClick = onBrowserLinkClick,
                    onCopyClick = onCopyClick
                )
            }
        }

        image.metadata.parentId?.let {
            item {
                TitleSummary(
                    title = "View parent image",
                    onClick = { onViewParentClick(it) },
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
                        onClick = { onViewRelatedClick(it) },
                        trailingIcon = {
                            ChevronRight()
                        }
                    )
                }
            }
        }
    }

    // In unified mode, these are displayed on the first page instead of this one.
    if (!unified) {
        LazyExpressiveGroup(useBox = true) {
            mainTagsItems(image, onTagClick, onTagLongClick)
        }
    }
}


@Suppress("unused")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.imageboardDataContentItems(
    image: Image,
    onLinkClick: (String) -> Unit, // Not currently used but keeping for consistency and possible future use
    onBrowserLinkClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit,
    unified: Boolean = false
) {
    if (!unified) {
        LazyExpressiveGroup(desiredTopPadding = null) {
            image.highestQualityFormatUrl.let {
                UrlItem(
                    label = "File URL",
                    url = it,
                    onLinkClick = onBrowserLinkClick,
                    onCopyClick = onCopyClick
                )
            }
        }
    }

    LazyExpressiveGroup(useBox = true) {
        if (unified) {
            mainTagsItems(image, onTagClick, onTagLongClick)
        }

        val groupedTags = image.metadataGroupedTagsOverride ?: image.metadata?.groupedTags

        groupedTags!!.filter {
            it.category !in setOf(
                TagCategory.ARTIST,
                TagCategory.CHARACTER,
                TagCategory.COPYRIGHT
            )
        }.forEach { group ->
            item {
                TagsContainer(
                    category = group.category,
                    tags = group.tags,
                    onTagClick = onTagClick,
                    onTagLongClick = onTagLongClick
                )
            }
        }
    }
}


private fun ExpressiveGroupScope.UrlItem(
    label: String,
    url: String,
    onLinkClick: ((String) -> Unit)? = null,
    onCopyClick: ((String) -> Unit)? = null
) {
    item {
        TitleSummary(
            title = label,
            summary = url,
            onClick = onLinkClick?.let { { onLinkClick(url) } },
            trailingIcon = onCopyClick?.let {
                {
                    CopyIcon(label) {
                        onCopyClick(url)
                    }
                }
            }
        )
    }
}


private fun ExpressiveGroupScope.mainTagsItems(
    image: Image,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit
) {
    val artists = image.metadataArtistsOverride ?: image.metadata?.artists
    val groupedTags = image.metadataGroupedTagsOverride ?: image.metadata?.groupedTags

    artists!!.takeIf { it.isNotEmpty() }?.let {
        item {
            TagsContainer(
                category = TagCategory.ARTIST,
                tags = it,
                onTagClick = onTagClick,
                onTagLongClick = onTagLongClick
            )
        }
    }

    groupedTags!!.filter {
        it.category in setOf(TagCategory.CHARACTER, TagCategory.COPYRIGHT)
    }.forEach { group ->
        item {
            TagsContainer(
                category = group.category,
                tags = group.tags,
                onTagClick = onTagClick,
                onTagLongClick = onTagLongClick
            )
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
    onTagClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit
) {
    val maxLines = 11 // 10 but apparently the expand indicator is included in this figure so 11
    val prefs = LocalPreferences.current
    var showAll by rememberSaveable { mutableStateOf(false) }

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
            ),
            modifier = Modifier.animateContentSize(),
            maxLines = if (!showAll) maxLines else Int.MAX_VALUE,
            overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                expandIndicator = {
                    ExpandCollapseRow(
                        label = "Show all (${tags.size})",
                        onClick = { showAll = true }
                    )
                },
                collapseIndicator = {
                    ExpandCollapseRow(
                        label = "Show less",
                        onClick = { showAll = false }
                    )
                },
                minRowsToShowCollapse = maxLines + 1
            )
        ) { index ->
            val tag = tags[index]
            CombinedClickableFilterChip(
                label = { Text(text = tag, maxLines = 1) },
                warning = tag in prefs.blockedTags,
                onClick = { onTagClick(tag) },
                onLongClick = { onTagLongClick(tag) }
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandCollapseRow(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TINY_SPACER.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(
            onClick = onClick
        ) {
            Text(label)
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
    intent.component = ComponentName(
        context,
        MainActivity::class.java
    )
    return intent
}
