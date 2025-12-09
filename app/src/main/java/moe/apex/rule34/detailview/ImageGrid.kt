package moe.apex.rule34.detailview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import moe.apex.rule34.image.Image
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.util.NavBarHeightVerticalSpacer
import moe.apex.rule34.util.PullToRefreshController
import moe.apex.rule34.util.SMALL_SPACER
import moe.apex.rule34.util.largerShape


private const val MIN_IMAGE_HEIGHT = 96
private const val MAX_IMAGE_HEIGHT = 280
private const val MIN_CELL_WIDTH   = 120
private const val MAX_CELL_WIDTH   = 144


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGrid(
    modifier: Modifier = Modifier,
    staggeredGridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    uniformGridState: LazyGridState = rememberLazyGridState(),
    images: List<Image>,
    onImageClick: (Int, Image) -> Unit,
    noImagesContent: @Composable () -> Unit = { NoImages() },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    filterComposable: (@Composable () -> Unit)? = null,
    pullToRefreshController: PullToRefreshController? = null,
    doneInitialLoad: Boolean = true,
    onEndReached: (suspend () -> Unit)? = null
) {
    val prefs = LocalPreferences.current

    @Composable
    fun Container(content: @Composable () -> Unit) {
        if (pullToRefreshController != null) {
            PullToRefreshBox(
                modifier = modifier,
                isRefreshing = pullToRefreshController.isRefreshing,
                state = pullToRefreshController.state,
                onRefresh = pullToRefreshController::refresh,
                indicator = {
                    pullToRefreshController.indicator(this, pullToRefreshController)
                },
                content = { content() }
            )
        } else {
            content()
        }
    }

    Container {
        if (prefs.useStaggeredGrid) {
            StaggeredImageGrid(
                modifier = if (pullToRefreshController == null) modifier else Modifier,
                gridState = staggeredGridState,
                contentPadding = contentPadding,
                filterComposable = filterComposable,
                images = images,
                noImagesContent = noImagesContent,
                onImageClick = onImageClick,
                onEndReached = onEndReached
            )
        } else {
            UniformImageGrid(
                modifier = if (pullToRefreshController == null) modifier else Modifier,
                gridState = uniformGridState,
                contentPadding = contentPadding,
                filterComposable = filterComposable,
                images = images,
                noImagesContent = noImagesContent,
                onImageClick = onImageClick,
                onEndReached = onEndReached
            )
        }
    }

    AnimatedVisibility (
        visible = !doneInitialLoad,
        enter = EnterTransition.None,
        exit = fadeOut(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LinearProgressIndicator(
                modifier = modifier
                    .fillMaxWidth()
            )
        }

        LaunchedEffect(doneInitialLoad) {
            if (doneInitialLoad) {
                staggeredGridState.requestScrollToItem(0)
                uniformGridState.requestScrollToItem(0)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaggeredImageGrid(
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues,
    filterComposable: (@Composable () -> Unit)? = null,
    images: List<Image>,
    noImagesContent: @Composable () -> Unit,
    onImageClick: (Int, Image) -> Unit,
    onEndReached: (suspend () -> Unit)? = null
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(MIN_CELL_WIDTH.dp),
        state = gridState,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp),
        verticalItemSpacing = SMALL_SPACER.dp
    ) {
        filterComposable?.let {
            item(key = "ratings-filter", span = StaggeredGridItemSpan.FullLine ) {
                it()
            }
        }

        itemsIndexed(images, key = { _, image -> image.previewUrl }) { index, image ->
            StaggeredImagePreviewContainer(image, index, onImageClick)
        }

        onEndReached?.let {
            item(key = "end-reached") {
                LaunchedEffect(Unit) {
                    it()
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
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues,
    filterComposable: (@Composable () -> Unit)? = null,
    images: List<Image>,
    noImagesContent: @Composable () -> Unit,
    onImageClick: (Int, Image) -> Unit,
    onEndReached: (suspend () -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(MIN_CELL_WIDTH.dp),
        state = gridState,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp),
        verticalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp)
    ) {
        filterComposable?.let {
            item(key = "ratings-filter", span = { GridItemSpan(maxLineSpan) }) {
                it()
            }
        }

        itemsIndexed(images, key = { _, image -> image.previewUrl }) { index, image ->
            ImagePreviewContainer(image, index, onImageClick)
        }

        onEndReached?.let {
            item(key = "end-reached") {
                LaunchedEffect(Unit) {
                    it()
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
fun NoImages() {
    Text(
        text = "No images :(",
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
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
            GifBadge()
        }
    }
}


@Composable
private fun ImagePreview(
    modifier: Modifier = Modifier,
    image: Image,
    index: Int,
    onImageClick: (Int, Image) -> Unit
) {
    val context = LocalContext.current
    val model = remember { ImageRequest.Builder(context)
        .data(image.previewUrl)
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
            GifBadge()
        }
    }
}


@Composable
private fun GifBadge() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SMALL_SPACER.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "GIF",
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
