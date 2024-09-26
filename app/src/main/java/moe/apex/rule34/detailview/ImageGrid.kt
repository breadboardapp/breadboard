package moe.apex.rule34.detailview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import moe.apex.rule34.image.Image
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.prefs
import moe.apex.rule34.util.FullscreenLoadingSpinner


@Composable
fun ImageGrid(
    modifier: Modifier = Modifier,
    shouldShowLargeImage: MutableState<Boolean>,
    initialPage: MutableIntState,
    showFilter: Boolean = false,
    images: List<Image>,
    onEndReached: () -> Unit = { }
) {
    val preferencesRepository = LocalContext.current.prefs
    val prefs = LocalPreferences.current
    val lazyGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val wantedSites = prefs.favouritesFilter
    val wantedImages = images.filter { it.imageSource in prefs.favouritesFilter }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        state = lazyGridState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showFilter) {
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(12.dp)) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .height(FilterChipDefaults.Height)
                        .clip(FilterChipDefaults.shape)
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (site in ImageSource.entries) {
                            FilterChip(
                                selected = site in wantedSites,
                                onClick = {
                                    scope.launch {
                                        if (site in wantedSites) preferencesRepository.removeFavouritesFilter(site)
                                        else preferencesRepository.addFavouritesFilter(site)
                                    }
                                },
                                label = { Text(site.description) },
                                leadingIcon = {
                                    AnimatedVisibility(site in wantedSites) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(wantedImages, key = { image -> image.previewUrl }) { image ->
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

        if (wantedImages.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "No images :(",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(
                modifier = Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }
}

