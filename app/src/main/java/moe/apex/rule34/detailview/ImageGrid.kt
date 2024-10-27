package moe.apex.rule34.detailview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.prefs
import moe.apex.rule34.util.CHIP_SPACING
import moe.apex.rule34.util.FullscreenLoadingSpinner
import moe.apex.rule34.util.HorizontallyScrollingChipsWithLabels
import moe.apex.rule34.util.NavBarHeightVerticalSpacer


@Composable
fun ImageGrid(
    modifier: Modifier = Modifier,
    images: List<Image>,
    onImageClick: (Int, Image) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showSourceFilter: Boolean = false,
    showRatingFilter: Boolean = false,
    initialLoad: (suspend () -> Unit)? = null,
    onEndReached: suspend () -> Unit = { }
) {
    val preferencesRepository = LocalContext.current.prefs
    val prefs = LocalPreferences.current
    val lazyGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var doneInitialLoad by remember { mutableStateOf(initialLoad == null) }
    val layoutDirection = LocalLayoutDirection.current

    val labels = mutableListOf<String>()
    val chips = mutableListOf<List<@Composable () -> Unit>>()
    if (showSourceFilter) {
        labels.add("Source")
        chips.add(ImageSource.entries.map { {
            FilterChip(
                selected = it in prefs.favouritesFilter,
                label = { Text(it.description) },
                onClick = {
                    scope.launch {
                        if (it in prefs.favouritesFilter) preferencesRepository.removeFavouritesFilter(it)
                        else preferencesRepository.addFavouritesFilter(it)
                    }
                }
            )
        } })
    }
    if (showRatingFilter) {
        labels.add("Ratings")
        chips.add(ImageRating.entries.map { {
            FilterChip(
                selected = it in prefs.favouritesRatingsFilter,
                label = { Text(it.label) },
                onClick = {
                    scope.launch {
                        if (it in prefs.favouritesRatingsFilter) preferencesRepository.removeFavouritesRatingFilter(it)
                        else preferencesRepository.addFavouritesRatingFilter(it)
                    }
                }
            )
        } })
    }

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
        if (showSourceFilter || showRatingFilter) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontallyScrollingChipsWithLabels(
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val sidePadding = contentPadding.calculateStartPadding(layoutDirection).roundToPx()
                            val placeable = measurable.measure(constraints.offset(horizontal = sidePadding))
                            layout (
                                width = placeable.width - sidePadding,
                                height = placeable.height
                            ) {
                                placeable.placeRelative(0, 0)
                            }
                        } // https://stackoverflow.com/a/75336645
                        .padding(bottom = 4.dp),
                    endPadding = ((16 - CHIP_SPACING).dp),
                    labels = labels,
                    content = chips
                )
            }
        }
        else {
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
