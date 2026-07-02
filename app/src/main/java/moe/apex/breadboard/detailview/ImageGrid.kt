package moe.apex.breadboard.detailview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import moe.apex.breadboard.image.Image
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.util.NavBarHeightVerticalSpacer
import moe.apex.breadboard.util.PullToRefreshController
import moe.apex.breadboard.util.SMALL_SPACER
import moe.apex.breadboard.util.WideLinearWavyProgressIndicator
import moe.apex.breadboard.util.largerShape


private const val MIN_IMAGE_HEIGHT = 96
private const val MAX_IMAGE_HEIGHT = 280
private const val MIN_CELL_WIDTH   = 120
private const val MAX_CELL_WIDTH   = 144


interface ImageGridHeaderScope {
    fun item(content: @Composable () -> Unit)
}


class ImageGridHeaderScopeImpl : ImageGridHeaderScope {
    val items = mutableListOf<@Composable () -> Unit>()

    override fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}


object FlexibleImageGridDefaults {
    @Composable
    fun WideLoadingIndicator() = WideLinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())


    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun RoundLoadingIndicator() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            LoadingIndicator()
        }
    }


    @Composable
    fun NoImages() {
        Text(
            text = "No images :(",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
private fun ImageGridPullToRefreshContainer(
    modifier: Modifier = Modifier,
    pullToRefreshController: PullToRefreshController?,
    content: @Composable () -> Unit
) {
    if (pullToRefreshController != null) {
        PullToRefreshBox(
            modifier = modifier,
            isRefreshing = pullToRefreshController.isRefreshing,
            state = pullToRefreshController.state,
            onRefresh = pullToRefreshController::refresh,
            indicator = {
                pullToRefreshController.indicator(this, pullToRefreshController)
            },
            enabled = pullToRefreshController.enabled,
            content = { content() }
        )
    } else {
        content()
    }
}


@Composable
private fun LoadingIndicatorContainer(
    modifier: Modifier = Modifier,
    doneInitialLoad: Boolean,
    contentPadding: PaddingValues,
    onInitialLoadCompleted: suspend () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = !doneInitialLoad,
        enter = EnterTransition.None,
        exit = fadeOut(),
    ) {
        Box(
            modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }

        LaunchedEffect(doneInitialLoad) {
            if (doneInitialLoad) {
                onInitialLoadCompleted()
            }
        }
    }
}


@Composable
fun FlexibleImageGrid(
    staggered: Boolean,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    images: List<Image>,
    onImageClick: (Int, Image) -> Unit,
    noImagesContent: @Composable () -> Unit = FlexibleImageGridDefaults::NoImages,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    headerItems: (ImageGridHeaderScope.() -> Unit)? = null,
    pullToRefreshController: PullToRefreshController? = null,
    doneInitialLoad: Boolean = true,
    loadingIndicator: @Composable () -> Unit = FlexibleImageGridDefaults::WideLoadingIndicator,
    onEndReached: (suspend () -> Unit)? = null
) {
    FlexibleImageGrid(
        gridState = if (staggered) rememberLazyStaggeredGridState() else rememberLazyGridState(),
        modifier = modifier,
        userScrollEnabled = userScrollEnabled,
        images = images,
        onImageClick = onImageClick,
        noImagesContent = noImagesContent,
        contentPadding = contentPadding,
        headerItems = headerItems,
        pullToRefreshController = pullToRefreshController,
        doneInitialLoad = doneInitialLoad,
        loadingIndicator = loadingIndicator,
        onEndReached = onEndReached
    )
}


@Composable
fun FlexibleImageGrid(
    gridState: ScrollableState,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    images: List<Image>,
    onImageClick: (Int, Image) -> Unit,
    noImagesContent: @Composable () -> Unit = FlexibleImageGridDefaults::NoImages,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    headerItems: (ImageGridHeaderScope.() -> Unit)? = null,
    pullToRefreshController: PullToRefreshController? = null,
    doneInitialLoad: Boolean = true,
    loadingIndicator: @Composable () -> Unit = FlexibleImageGridDefaults::WideLoadingIndicator,
    onEndReached: (suspend () -> Unit)? = null
) {
    val scope = ImageGridHeaderScopeImpl()
    if (headerItems != null) {
        scope.headerItems()
    }

    when (gridState) {
        is LazyStaggeredGridState -> {
            Box {
                ImageGridPullToRefreshContainer(
                    modifier = modifier,
                    pullToRefreshController = pullToRefreshController
                ) {
                    StaggeredImageGrid(
                        modifier = if (pullToRefreshController == null) modifier else Modifier,
                        userScrollEnabled = userScrollEnabled,
                        gridState = gridState,
                        contentPadding = contentPadding,
                        headerItems = scope.items,
                        images = images,
                        noImagesContent = noImagesContent,
                        onImageClick = onImageClick,
                        onEndReached = onEndReached
                    )
                }

                LoadingIndicatorContainer(
                    modifier = modifier,
                    doneInitialLoad = doneInitialLoad,
                    contentPadding = contentPadding,
                    onInitialLoadCompleted = { gridState.requestScrollToItem(0) },
                    content = loadingIndicator
                )
            }
        }

        is LazyGridState -> {
            ImageGridPullToRefreshContainer(
                modifier = modifier,
                pullToRefreshController = pullToRefreshController
            ) {
                Box {
                    UniformImageGrid(
                        modifier = if (pullToRefreshController == null) modifier else Modifier,
                        userScrollEnabled = userScrollEnabled,
                        gridState = gridState,
                        contentPadding = contentPadding,
                        headerItems = scope.items,
                        images = images,
                        noImagesContent = noImagesContent,
                        onImageClick = onImageClick,
                        onEndReached = onEndReached
                    )

                    LoadingIndicatorContainer(
                        modifier = modifier,
                        doneInitialLoad = doneInitialLoad,
                        contentPadding = contentPadding,
                        onInitialLoadCompleted = { gridState.scrollToItem(0) },
                        content = loadingIndicator
                    )
                }
            }
        }

        else -> {
            throw IllegalArgumentException("gridState must be either LazyStaggeredGridState or LazyGridState")
        }
    }
}


@Deprecated(
    message = "Use FlexibleImageGrid, which allows for passing in multiple header items, " +
              "custom loading indicators, and controlling whether user scroll is allowed, instead.",
    replaceWith = ReplaceWith("FlexibleImageGrid")
)
@Composable
fun ImageGrid(
    modifier: Modifier = Modifier,
    staggeredGridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    uniformGridState: LazyGridState = rememberLazyGridState(),
    images: List<Image>,
    onImageClick: (Int, Image) -> Unit,
    noImagesContent: @Composable () -> Unit = FlexibleImageGridDefaults::NoImages,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    filterComposable: (@Composable () -> Unit)? = null,
    pullToRefreshController: PullToRefreshController? = null,
    doneInitialLoad: Boolean = true,
    onEndReached: (suspend () -> Unit)? = null
) {
    val prefs = LocalPreferences.current

    FlexibleImageGrid(
        gridState = if (prefs.useStaggeredGrid) staggeredGridState else uniformGridState,
        modifier = modifier,
        images = images,
        onImageClick = onImageClick,
        noImagesContent = noImagesContent,
        contentPadding = contentPadding,
        headerItems = filterComposable?.let { { item { it() }} },
        pullToRefreshController = pullToRefreshController,
        doneInitialLoad = doneInitialLoad,
        onEndReached = onEndReached
    )
}


@Composable
private fun StaggeredImageGrid(
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues,
    headerItems: (List<@Composable () -> Unit>)? = null,
    images: List<Image>,
    noImagesContent: @Composable () -> Unit,
    onImageClick: (Int, Image) -> Unit,
    onEndReached: (suspend () -> Unit)? = null
) {
    val scope = rememberCoroutineScope()

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(MIN_CELL_WIDTH.dp),
        state = gridState,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp),
        verticalItemSpacing = SMALL_SPACER.dp,
        userScrollEnabled = userScrollEnabled
    ) {
        headerItems?.forEachIndexed { index, element ->
            item(key = "header-$index", span = StaggeredGridItemSpan.FullLine) {
                element()
            }
        }

        itemsIndexed(images, key = { _, image -> image.previewUrl }) { index, image ->
            StaggeredImagePreviewContainer(image, index, onImageClick)
        }

        onEndReached?.let {
            item(key = "end-reached") {
                LaunchedEffect(Unit) {
                    scope.launch {
                        it()
                    }
                }
            }
        }

        if (images.isEmpty()) {
            item(key = "no-images", span = StaggeredGridItemSpan.FullLine) {
                noImagesContent()
            }
        }

        item(key = "spacer", span = StaggeredGridItemSpan.FullLine) {
            NavBarHeightVerticalSpacer()
        }
    }
}


@Composable
private fun UniformImageGrid(
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues,
    headerItems: List<@Composable () -> Unit>? = null,
    images: List<Image>,
    noImagesContent: @Composable () -> Unit,
    onImageClick: (Int, Image) -> Unit,
    onEndReached: (suspend () -> Unit)? = null
) {
    val scope = rememberCoroutineScope()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(MIN_CELL_WIDTH.dp),
        state = gridState,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp),
        verticalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp),
        userScrollEnabled = userScrollEnabled
    ) {
        headerItems?.forEachIndexed { index, element ->
            item(key = "header-$index", span = { GridItemSpan(maxLineSpan) }) {
                element()
            }
        }

        itemsIndexed(images, key = { _, image -> image.previewUrl }) { index, image ->
            ImagePreviewContainer(image, index, onImageClick)
        }

        onEndReached?.let {
            item(key = "end-reached") {
                LaunchedEffect(Unit) {
                    scope.launch {
                        it()
                    }
                }
            }
        }

        if (images.isEmpty()) {
            item(key = "no-images", span = { GridItemSpan(maxLineSpan) }) {
                noImagesContent()
            }
        }

        item(key = "spacer", span = { GridItemSpan(maxLineSpan) }) {
            NavBarHeightVerticalSpacer()
        }
    }
}


@Composable
private fun LazyGridItemScope.ImagePreviewContainer(
    image: Image,
    index: Int,
    onImageClick: (Int, Image) -> Unit
) {
    Box(
        modifier = Modifier
            .animateItem(
                fadeOutSpec = null,
                placementSpec = null
            )
            .fillMaxWidth()
            .widthIn(MAX_CELL_WIDTH.dp)
            .aspectRatio(1f)
            .clip(largerShape)
    ) {
        ImagePreview(
            modifier = Modifier.fillMaxSize(),
            image = image,
            index = index,
            onImageClick = onImageClick
        )
        if (image.fileFormat == "gif") {
            FormatBadge("GIF")
        } else if (image.isVideo) {
            FormatBadge("Video")
        }
    }
}


@Composable
fun ImagePreview(
    modifier: Modifier = Modifier,
    image: Image,
    index: Int,
    onImageClick: (Int, Image) -> Unit
) {
    val context = LocalContext.current

    val headersBuilder = remember {
        NetworkHeaders.Builder()
            .set("Referer", image.imageSource.imageBoard.baseUrl)
    }
    val model = remember { ImageRequest.Builder(context)
        .data(image.previewUrl)
        .httpHeaders(headersBuilder.build())
        .crossfade(true)
        .build()
    }

    AsyncImage(
        model = model,
        contentDescription = "Image",
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.clickable { onImageClick(index, image) }
    )
}


@Composable
private fun LazyStaggeredGridItemScope.StaggeredImagePreviewContainer(
    image: Image,
    index: Int,
    onImageClick: (Int, Image) -> Unit
) {
    Box(
        modifier = Modifier
            .animateItem(
                fadeOutSpec = null,
                placementSpec = null
            )
            .widthIn(min = MIN_CELL_WIDTH.dp, max = MAX_CELL_WIDTH.dp)
            .heightIn(min = MIN_IMAGE_HEIGHT.dp, max = MAX_IMAGE_HEIGHT.dp)
            .clip(largerShape),
        contentAlignment = Alignment.TopEnd,
        propagateMinConstraints = true
    ) {
        ImagePreview(
            modifier = Modifier
                .aspectRatio(image.aspectRatio ?: 1f)
                .requiredHeightIn(min = MIN_IMAGE_HEIGHT.dp)
                .fillMaxWidth(),
            image = image,
            index = index,
            onImageClick = onImageClick
        )
        /* This whole required/widthIn/heightIn thing is awkward but it seems like the only simple
           way to respect the aspect ratio of the image while enforcing a minimum/maximum size for
           very tall or wide images. While widthIn/heightIn cannot 'override' the aspectRatio,
           requiredWidthIn/requiredHeightIn can. */
        if (image.fileFormat == "gif") {
            FormatBadge("GIF")
        } else if (image.isVideo) {
            FormatBadge("Video")
        }
    }
}

@Composable
private fun FormatBadge(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SMALL_SPACER.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
                .padding(vertical = 3.dp, horizontal = 6.dp)
        )
    }
}
