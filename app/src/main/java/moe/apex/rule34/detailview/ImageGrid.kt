package moe.apex.rule34.detailview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import moe.apex.rule34.image.Image
import moe.apex.rule34.util.FullscreenLoadingSpinner


@Composable
fun ImageGrid(
    modifier: Modifier = Modifier,
    shouldShowLargeImage: MutableState<Boolean>,
    initialPage: MutableIntState,
    images: List<Image>,
    onEndReached: () -> Unit = { }
) {
    val lazyGridState = rememberLazyGridState()

    if (images.isNotEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(128.dp),
            state = lazyGridState,
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(images, key = { image -> image.previewUrl }) { image ->
                Surface(
                    Modifier
                        .aspectRatio(1f)
                        .widthIn(max = 144.dp)
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
                            .clip(RoundedCornerShape(12.dp))
                            .fillMaxSize()
                            .clickable {
                                initialPage.intValue = images.indexOf(image)
                                shouldShowLargeImage.value = true
                            },
                    )
                }
            }

            item { onEndReached() }

            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
            }
        }
    } else {
        Text(
            text = "No images :(",
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 48.dp)
        )
    }
}

