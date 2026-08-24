package moe.apex.breadboard.saucenao

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.apex.breadboard.R
import moe.apex.breadboard.detailview.FlexibleImageGridDefaults
import moe.apex.breadboard.social.SocialSite
import moe.apex.breadboard.social.sortSocialSites
import moe.apex.breadboard.image.ImageBoardRequirement
import moe.apex.breadboard.largeimageview.OffsetBasedLargeImageView
import moe.apex.breadboard.navigation.ImageView
import moe.apex.breadboard.preferences.Experiment
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.util.BasicExpressiveContainer
import moe.apex.breadboard.util.CHIP_SPACING
import moe.apex.breadboard.util.LARGE_SPACER
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.ListItemPosition
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.SMALL_SPACER
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.WideLinearWavyProgressIndicator
import moe.apex.breadboard.util.bouncyAnimationSpec
import moe.apex.breadboard.util.filterChipSolidColor
import moe.apex.breadboard.util.largerShapeCornerSize
import moe.apex.breadboard.util.navBarHeight
import moe.apex.breadboard.util.openUrl
import moe.apex.breadboard.util.showToast
import moe.apex.breadboard.viewmodel.ResultsState
import moe.apex.breadboard.viewmodel.SaucenaoResultsViewModel


private val MATCH_BOX_HEIGHT = 24.dp
private val TITLE_HEIGHT = LARGE_SPACER.dp


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun SauceNaoResultsScreen(
    navController: NavController,
    imageUrl: String,
    fileUri: String = ""
) {
    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val blur = prefs.isExperimentEnabled(Experiment.IMMERSIVE_UI_EFFECTS)

    val viewModel: SaucenaoResultsViewModel = viewModel()
    val viewableImages by viewModel.viewableImages.collectAsState()
    var selectedImages by remember { mutableStateOf(viewableImages) }
    val state by viewModel.resultsState.collectAsState()

    var isImageViewerActive by remember { mutableStateOf(false) }

    fun onViewInBreadboard(vararg ids: Int?) {
        val ids = ids.filterNotNull()
        selectedImages = viewableImages.filter { it.id?.toInt() in ids }

        if (selectedImages.all {
                it.imageSource.imageBoard.apiKeyRequirement == ImageBoardRequirement.REQUIRED &&
                    prefs.authFor(it.imageSource, context) == null
        } ) {
            showToast(context, "Unable to open due to missing API keys.")
        } else {
            isImageViewerActive = true
        }
    }

    suspend fun performSearch() {
        if (fileUri.isNotEmpty()) {
            val uri = fileUri.toUri()
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Could not open file")
            val fileName = uri.lastPathSegment ?: "image.jpg"
            viewModel.performFileSearch(
                fileBytes = fileBytes,
                fileName = fileName,
                mimeType = mimeType,
                apiKey = prefs.saucenaoApiKey,
                allowNsfw = prefs.saucenaoAllowNsfw,
                authFactory = {
                    prefs.authFor(it, context)
                }
            )
        } else {
            viewModel.performSearch(
                imageUrl = imageUrl,
                apiKey = prefs.saucenaoApiKey,
                allowNsfw = prefs.saucenaoAllowNsfw,
                authFactory = {
                    prefs.authFor(it, context)
                }
            )
        }
    }

    LaunchedEffect(imageUrl, fileUri) {
        if (state !is ResultsState.Success) {
            performSearch()
        }
    }

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "SauceNAO",
                scrollBehavior = scrollBehavior,
                navController = navController
            )
        },
        addBottomPadding = false,
        blur = isImageViewerActive && blur,
    ) { padding ->
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { currentState ->
            when (currentState) {
                is ResultsState.Loading -> {
                    Box(modifier = Modifier
                        .padding(SMALL_LARGE_SPACER.dp)) {
                        WideLinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }

                is ResultsState.Error -> {
                    showToast(context, "${currentState.message} ${currentState.statusCode}")
                    navController.popBackStack()
                }

                is ResultsState.Success -> {
                    if (currentState.groups.isEmpty()) {
                        FlexibleImageGridDefaults.NoImages("No matches found :(")
                    } else {
                        var expandedGroupIndex by remember { mutableIntStateOf(-1) }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = PaddingValues(
                                top = MEDIUM_SPACER.dp,
                                start = MEDIUM_SPACER.dp,
                                end = MEDIUM_SPACER.dp,
                                bottom = navBarHeight + MEDIUM_SPACER.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            currentState.groups.forEachIndexed { groupIndex, group ->
                                // Primary result with merged links
                                val isExpanded = expandedGroupIndex == groupIndex
                                item(key = groupIndex.toString()) {
                                    BasicExpressiveContainer(
                                        modifier = Modifier
                                            .animateItem(placementSpec = bouncyAnimationSpec())
                                            .padding(
                                                bottom = if (expandedGroupIndex == groupIndex + 1) {
                                                    LARGE_SPACER - 2
                                                } else {
                                                    0
                                                }.dp
                                            ),
                                        // I *think* this covers all bases but I probably forgot something lol
                                        position = when {
                                            isExpanded -> ListItemPosition.TOP // If this result is expanded
                                            currentState.header.resultsCount == 1 -> ListItemPosition.SINGLE_ELEMENT // If this is the only result
                                            groupIndex == 0 && expandedGroupIndex == 1 -> ListItemPosition.SINGLE_ELEMENT // If this is the first result and the result below this is expanded
                                            groupIndex == 0 -> ListItemPosition.TOP // If this is the first result and the result below this is not expanded
                                            groupIndex == currentState.groups.lastIndex && expandedGroupIndex == groupIndex - 1 -> ListItemPosition.SINGLE_ELEMENT // If this is the last result and the one above this is expanded
                                            groupIndex - 1 == expandedGroupIndex -> ListItemPosition.TOP // If this is not the last result, but the result above this is expanded
                                            expandedGroupIndex == groupIndex + 1 -> ListItemPosition.BOTTOM // If this is not the first result and the below this is expanded.
                                            groupIndex == currentState.groups.lastIndex -> ListItemPosition.BOTTOM // If this is the last result and the result above is not expanded.
                                            else -> ListItemPosition.MIDDLE // Everything else
                                        }
                                    ) {
                                        SauceNaoResultItem(
                                            result = group.primaryResult,
                                            isBestMatch = groupIndex == 0 && group.primaryResult.header.similarity >= 80f,
                                            urls = group.mergedUrls,
                                            imageBoards = group.mergedImageBoards,
                                            onOpenUrl = { url -> openUrl(context, url) },
                                            onViewInBreadboard = {
                                                onViewInBreadboard(
                                                    group.primaryResult.data.danbooruId,
                                                    group.primaryResult.data.gelbooruId,
                                                    group.primaryResult.data.yandereId
                                                )
                                            },
                                            relatedCount = group.relatedResults.size,
                                            isExpanded = isExpanded,
                                            onToggleExpand = {
                                                expandedGroupIndex = if (isExpanded) {
                                                    -1
                                                } else {
                                                    groupIndex
                                                }
                                            }
                                        )
                                    }
                                }

                                if (isExpanded) {
                                    // Related results (shown when expanded)
                                    itemsIndexed(
                                        items = group.relatedResults,
                                        key = { index, _ -> "$groupIndex-$index" }
                                    ) { index, related ->
                                        val isLastRelated = index == group.relatedResults.lastIndex
                                        BasicExpressiveContainer(
                                            modifier = Modifier
                                                .animateItem()
                                                .padding(
                                                    bottom = if (isLastRelated) {
                                                        LARGE_SPACER - 2
                                                    } else {
                                                        0
                                                    }.dp
                                                ),
                                            position = when {
                                                isLastRelated -> ListItemPosition.BOTTOM
                                                else -> ListItemPosition.MIDDLE
                                            }
                                        ) {
                                            SauceNaoResultItem(
                                                result = related,
                                                isBestMatch = false,
                                                onOpenUrl = { url ->
                                                    openUrl(
                                                        context,
                                                        url
                                                    )
                                                },
                                                onViewInBreadboard = {
                                                    onViewInBreadboard(
                                                        group.primaryResult.data.danbooruId,
                                                        group.primaryResult.data.gelbooruId,
                                                        group.primaryResult.data.yandereId
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Total displayed results across all groups
                            val totalResults =
                                currentState.groups.sumOf { 1 + it.relatedResults.size }
                            if (currentState.header.resultsCount != totalResults) {
                                item {
                                    Text(
                                        text = "Some potentially explicit results have been hidden.\n" +
                                                "Adjust this in Content settings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .animateItem()
                                            .fillMaxWidth()
                                            .padding(MEDIUM_SPACER.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /* We're not setting onImageUpdate here because the viewmodel's performSearch handles fetching
       the tag groups already (and those are the primary use case for onImageUpdate). */
    OffsetBasedLargeImageView(
        navController = navController,
        isActive = isImageViewerActive,
        initialSelectedImageIndex = 0,
        allImages = selectedImages,
        onActiveStateChanged = { isImageViewerActive = it }
    )
}


/**
 * A single SauceNAO result item.
 *
 * When used as a primary result in a group, pass [urls] and [imageBoards] for the merged data,
 * [relatedCount] for the number of related results, [isExpanded] and [onToggleExpand] for the
 * expand/collapse chevron.
 *
 * When used as a related result, omit the optional parameters to use the result's own data.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SauceNaoResultItem(
    modifier: Modifier = Modifier,
    result: SauceNaoResult,
    isBestMatch: Boolean,
    onOpenUrl: (String) -> Unit,
    onViewInBreadboard: () -> Unit,
    urls: Map<SocialSite, List<String>> = result.data.mapUrls(),
    imageBoards: List<ImageView> = result.data.parseImageBoards(),
    relatedCount: Int = 0,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Box {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        start = SMALL_LARGE_SPACER.dp,
                        end = SMALL_LARGE_SPACER.dp,
                        top = SMALL_LARGE_SPACER.dp
                        // Bottom will be handled by either the URL chips or the image, whichever has the lowest point in the UI.
                    ),
                horizontalArrangement = Arrangement.spacedBy(SMALL_LARGE_SPACER.dp)
            ) {
                /* TODO: Make thumbnail tappable?
                    Will need adjustments to the image viewer to support non-Image items. */
                if (result.header.thumbnail.isNotEmpty()) {
                    AsyncImage(
                        model = result.header.thumbnail,
                        contentDescription = "Result thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            /* A bit of manual work to ensure the image lines up with the text and
                               exactly one row of ext url chips.
                               A simpler way would be fillMaxHeight and IntrinsicSize.Min on the
                               parent Row, but that then introduces complications if we have more
                               than one row of url chips, so we'll just keep this.  */
                            .padding(bottom = SMALL_LARGE_SPACER.dp)
                            .size(MATCH_BOX_HEIGHT + MEDIUM_SPACER.dp + TITLE_HEIGHT + SMALL_SPACER.dp + 8.dp + FilterChipDefaults.Height)
                            .clip(RoundedCornerShape(largerShapeCornerSize))
                    )
                }

                Column(Modifier.weight(1f)) {
                    val similarity = result.header.similarity
                    val badgeColour = when {
                        similarity >= 80f -> Color(0xFF43A047) // Green 600
                        similarity >= 50f -> Color(0xFFF57C00) // Orange 700
                        else -> Color(0xFFE53935) // Red 600
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(
                                shape = CircleShape,
                                color = badgeColour.copy(alpha = 0.2f)
                            )
                            .height(MATCH_BOX_HEIGHT)
                            .padding(horizontal = SMALL_SPACER.dp)
                    ) {
                        Text(
                            text = "$similarity% " + if (isBestMatch) "⸱ Best match" else "match",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp
                        )
                    }

                    /* We're doing this to enforce a specific layout size for the text in order to
                       keep everything aligned.
                       The height of the text itself is allowed to overflow outside of this Box
                       to prevent clipping caused by CJK characters screwing with the expected
                       line height.

                       Will this cause issues if someone has a silly font size/dpi?
                       I'm sure we'll find out eventually. */
                    Box(
                        modifier = Modifier
                            .padding(top = MEDIUM_SPACER.dp, bottom = SMALL_SPACER.dp) // The site chips have their own padding already
                            .height(TITLE_HEIGHT),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("Art by ")
                                withStyle(
                                    style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                                ) {
                                    append(result.data.artistName)
                                }
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.kumbh)),
                            modifier = Modifier.wrapContentHeight(unbounded = true)
                        )
                    }

                    val sortedSocials = sortSocialSites(urls.keys)

                    if (sortedSocials.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp),
                            // Chips 8dp vertical yada yada you've heard it before
                            modifier = Modifier.padding(bottom = (SMALL_LARGE_SPACER - 8).dp)
                        ) {
                            for (site in sortedSocials) {
                                val siteUrls = urls[site]!!
                                for (url in siteUrls) {
                                    FilterChip(
                                        selected = true,
                                        onClick = { onOpenUrl(url) },
                                        label = {
                                            Text(
                                                text = if (site != SocialSite.OTHER) {
                                                    site.label
                                                } else {
                                                    url.toUri().host.toString()
                                                        .removePrefix("www.")
                                                }
                                            )
                                        },
                                        border = null,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // We're putting this separately in the Box so it doesn't affect the layout of the rest.
            if (relatedCount > 0 && onToggleExpand != null) {
                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "chevron_rotation"
                )
                FilterChip(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = TINY_SPACER.dp, // They have 8dp vertical already
                            end = MEDIUM_SPACER.dp
                        ),
                    selected = isExpanded,
                    onClick = onToggleExpand,
                    shapes = FilterChipDefaults.shapes().copy(
                        shape = CircleShape,
                        selectedShape = FilterChipDefaults.shape
                    ),
                    colors = filterChipSolidColor.copy(
                        containerColor = filterChipSolidColor.selectedContainerColor,
                        labelColor = filterChipSolidColor.selectedLabelColor,
                        leadingIconColor = filterChipSolidColor.selectedLeadingIconColor
                    ),
                    border = null,
                    label = { Text("+$relatedCount") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) {
                                "Collapse $relatedCount related results"
                            } else {
                                "Expand $relatedCount related results"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = rotation
                            }
                        )
                    }
                )
            }
        }
        if (imageBoards.isNotEmpty()) {
            Button(
                onClick = onViewInBreadboard,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SMALL_LARGE_SPACER.dp,
                        end = SMALL_LARGE_SPACER.dp,
                        bottom = SMALL_LARGE_SPACER.dp
                    )
            ) {
                Text("Open in Breadboard")
            }
        }
    }
}
