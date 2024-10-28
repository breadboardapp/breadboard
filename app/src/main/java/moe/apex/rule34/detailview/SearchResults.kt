package moe.apex.rule34.detailview

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.prefs
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.util.AnimatedVisibilityLargeImageView
import moe.apex.rule34.util.CHIP_SPACING
import moe.apex.rule34.util.HorizontallyScrollingChipsWithLabels
import moe.apex.rule34.util.TitleBar
import moe.apex.rule34.util.withoutVertical


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResults(navController: NavController, searchQuery: String) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }
    val allImages = remember { mutableStateListOf<Image>() }
    var shouldKeepSearching by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val prefs = LocalPreferences.current
    val preferencesRepository = LocalContext.current.prefs
    val imageSource = prefs.imageSource.site
    val filterLocally = prefs.filterRatingsLocally
    var pageNumber by remember { mutableIntStateOf(imageSource.firstPageIndex) }

    val ratingRows: List<@Composable () -> Unit> = ImageRating.entries.filter { it != ImageRating.UNKNOWN }.map { {
        FilterChip(
            selected = it in prefs.ratingsFilter,
            label = { Text(it.label) },
            onClick = {
                scope.launch {
                    if (it in prefs.ratingsFilter) preferencesRepository.removeRatingFilter(it)
                    else preferencesRepository.addRatingFilter(it)
                }
            }
        )
    } }

    val imagesToDisplay = allImages.filter {
        if (prefs.filterRatingsLocally) it.metadata!!.rating in prefs.ratingsFilter
        else true
    }

    BreadboardTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TitleBar(
                    title = "Search results",
                    scrollBehavior = scrollBehavior,
                    navController = navController
                )
            }
        ) { padding ->
            ImageGrid(
                modifier = Modifier
                    .padding(padding.withoutVertical(top = false))
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                images = imagesToDisplay,
                onImageClick = { index, image ->
                    initialPage = index
                    shouldShowLargeImage.value = true
                },
                contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp),
                filterComposable = if (filterLocally) { {
                    HorizontallyScrollingChipsWithLabels(
                        modifier = Modifier.padding(bottom = 4.dp),
                        labels = listOf("Ratings"),
                        content = listOf(ratingRows)
                    )
                } } else null,
                initialLoad = {
                    withContext(Dispatchers.IO) {
                        try {
                            val newImages = imageSource.loadPage(searchQuery, pageNumber)
                            if (!allImages.addAll(newImages)) shouldKeepSearching = false
                            pageNumber++
                        } catch (e: Exception) {
                            Log.e("SearchResults", "Error loading initial images", e)
                            shouldKeepSearching = false
                        }
                    }
                }
            ) {
                if (shouldKeepSearching) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val newImages = imageSource.loadPage(searchQuery, pageNumber)
                            if (newImages.isNotEmpty()) {
                                pageNumber++
                                allImages.addAll(newImages)
                            } else {
                                shouldKeepSearching = false
                            }
                        } catch (e: Exception) {
                            Log.e("SearchResults", "Error loading new images", e)
                            shouldKeepSearching = false
                        }
                    }
                }
            }
        }
    }
    AnimatedVisibilityLargeImageView(shouldShowLargeImage, initialPage, imagesToDisplay)
}
