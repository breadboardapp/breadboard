package moe.apex.rule34.detailview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import moe.apex.rule34.util.FullscreenLoadingSpinner
import moe.apex.rule34.util.NavBarHeightVerticalSpacer


@Composable
fun ImageGrid(
    modifier: Modifier = Modifier,
    images: List<Image>,
    onImageClick: (Int, Image) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    filterComposable: (@Composable () -> Unit)? = null,
    initialLoad: (suspend () -> Unit)? = null,
    onEndReached: suspend () -> Unit = { }
) {
    val lazyGridState = rememberLazyGridState()
    var doneInitialLoad by remember { mutableStateOf(initialLoad == null) }

    if (!doneInitialLoad) {
        LaunchedEffect(Unit) {
            initialLoad!!.invoke()
            doneInitialLoad = true
        }
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(contentPadding)
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        state = lazyGridState,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (filterComposable != null) {
            item(span = { GridItemSpan(maxLineSpan) } ) {
                filterComposable()
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        itemsIndexed(images, key = { _, image -> image.previewUrl }) { index, image ->
            Surface(
                Modifier
                    .aspectRatio(1f)
                    .widthIn(max = 144.dp)
            ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(image.previewUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Image",
                        contentScale = ContentScale.Crop,
                        loading = { FullscreenLoadingSpinner() },
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onImageClick(index, image) },
                    )
                    if (image.fileFormat == "gif") {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .height(IntrinsicSize.Min)
                                .width(IntrinsicSize.Min)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GIF",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item { LaunchedEffect(Unit) { onEndReached() } }

        if (images.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "No images :(",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            NavBarHeightVerticalSpacer()
        }
    }
}
