package moe.apex.rule34.detailview

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageSource
import moe.apex.rule34.largeimageview.FullscreenLoadingSpinner


@Composable
@Suppress("UNUSED_PARAMETER")
fun ImageGrid(
    imageSource: ImageSource,
    navController: NavController,
    shouldShowLargeImage: MutableState<Boolean>,
    initialPage: MutableIntState,
    allImages: SnapshotStateList<Image>
) {
    val interactionSource = remember { MutableInteractionSource() }
    var doneInitialLoad by remember { mutableStateOf(false) }
    val lazyGridState = rememberLazyGridState()
    var pageNumber by remember { mutableIntStateOf(1) }
    var shouldKeepSearching by remember { mutableStateOf(true) }
    var isLoading = false

    if (!doneInitialLoad) {
        isLoading = true
        val newImages = imageSource.loadPage(0)
        if (!allImages.addAll(newImages)) {
            shouldKeepSearching = false
        }
        doneInitialLoad = true
        isLoading = false
    }

    if (allImages.size > 0) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(128.dp),
            state = lazyGridState,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allImages, key = { image -> image.previewUrl }) { image ->
                Surface(
                    Modifier
                        .padding(bottom = 8.dp)
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
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                onClick = {
                                    initialPage.intValue = allImages.indexOf(image)
                                    shouldShowLargeImage.value = true
                                }
                            ),
                    )
                }
            }

            item {
                if (shouldKeepSearching) {
                    if (!isLoading) {
                        isLoading = true
                        LaunchedEffect(true) {
                            launch(Dispatchers.IO) {
                                val newImages = imageSource.loadPage(pageNumber)
                                if (newImages.isNotEmpty()) {
                                    pageNumber++
                                    allImages.addAll(newImages)
                                } else {
                                    shouldKeepSearching = false
                                }
                                isLoading = false
                            }
                        }
                    }
                }
            }
        }
    } else {
        if (!shouldKeepSearching) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No results :(",
                    modifier = Modifier.padding(top = 48.dp)
                )
                shouldKeepSearching = false
            }
        }
    }
}

