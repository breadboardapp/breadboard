package moe.apex.rule34.detailview

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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import moe.apex.rule34.image.Image
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.util.FullscreenLoadingSpinner
import moe.apex.rule34.util.NavBarHeightVerticalSpacer
import moe.apex.rule34.util.SMALL_SPACER
import moe.apex.rule34.util.largerShape


private const val MIN_IMAGE_HEIGHT = 108
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
    onPullToRefresh: ((MutableState<Boolean>) -> Unit)? = null,
    initialLoad: (suspend () -> Unit)? = null,
    onEndReached: suspend () -> Unit = { }
) {
    val prefs = LocalPreferences.current
    var doneInitialLoad by remember { mutableStateOf(initialLoad == null || images.isNotEmpty()) }

    if (!doneInitialLoad) {
        LaunchedEffect(Unit) {
            initialLoad!!()
            doneInitialLoad = true
        }
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding)
        )
        return
    }

    @Composable
    fun Container(content: @Composable () -> Unit) {
        if (onPullToRefresh != null) {
            val ptrState = rememberPullToRefreshState()
            val isRefreshing = remember { mutableStateOf(false) }

            PullToRefreshBox(
                modifier = modifier,
                isRefreshing = isRefreshing.value,
                state = ptrState,
                onRefresh = { onPullToRefresh(isRefreshing) },
                content = { content() }
            )
        } else {
            content()
        }
    }

    Container {
        if (prefs.useStaggeredGrid) {
            StaggeredImageGrid(
                modifier = if (onPullToRefresh == null) modifier else Modifier,
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
                modifier = if (onPullToRefresh == null) modifier else Modifier,
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
    onEndReached: suspend () -> Unit = { }
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
            item(span = StaggeredGridItemSpan.FullLine ) { it() }
        }

        itemsIndexed(images, key = { _, image -> image.previewUrl }) { index, image ->
            StaggeredImagePreviewContainer(image, index, onImageClick)
        }

        item { LaunchedEffect(Unit) { onEndReached() } }

        if (images.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                noImagesContent()
            }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
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
    onEndReached: suspend () -> Unit = { }
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
            item(span = { GridItemSpan(maxLineSpan) }) { it() }
        }

        itemsIndexed(images, key = { _, image -> image.previewUrl }) { index, image ->
            ImagePreviewContainer(image, index, onImageClick)
        }

        item { LaunchedEffect(Unit) { onEndReached() } }

        if (images.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                noImagesContent()
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            NavBarHeightVerticalSpacer()
        }
    }
}

@Composable
private fun NoImages() {
    Text(
        text = "No images :(",
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}


@Composable
private fun ImagePreviewContainer(
    image: Image,
    index: Int,
    onImageClick: (Int, Image) -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(max = MAX_CELL_WIDTH.dp)
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
}


@Composable
private fun ImagePreview(
    modifier: Modifier = Modifier,
    image: Image,
    index: Int,
    onImageClick: (Int, Image) -> Unit
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(image.previewUrl)
            .crossfade(true)
            .build(),
        contentDescription = "Image",
        contentScale = ContentScale.Crop,
        loading = { FullscreenLoadingSpinner() },
        modifier = modifier.clickable { onImageClick(index, image) }
    )
}


@Composable
private fun StaggeredImagePreviewContainer(
    image: Image,
    index: Int,
    onImageClick: (Int, Image) -> Unit
) {
    Box(
        modifier = Modifier
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
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(4.dp)
                )
                .padding(vertical = 2.dp, horizontal = 4.dp)
        )
    }
}
