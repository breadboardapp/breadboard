package moe.apex.rule34.detailview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import moe.apex.rule34.image.Image
import moe.apex.rule34.preferences.Prefs
import moe.apex.rule34.prefs
import moe.apex.rule34.ui.theme.ProcrasturbatingTheme
import moe.apex.rule34.util.AnimatedVisibilityLargeImageView
import moe.apex.rule34.util.TitleBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResults(navController: NavController, searchQuery: String) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    val initialPage = remember { mutableIntStateOf(0) }
    val allImages = remember { mutableStateListOf<Image>() }
    var doneInitialLoad by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var shouldKeepSearching by remember { mutableStateOf(true) }
    var pageNumber by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()
    val imageSource = LocalContext.current.prefs.getPreferences
        .collectAsState(Prefs.DEFAULT).value.imageSource.site

    ProcrasturbatingTheme {
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
            if (!doneInitialLoad) {
                isLoading = true
                val newImages = imageSource.loadPage(searchQuery, 0)
                if (!allImages.addAll(newImages)) {
                    shouldKeepSearching = false
                }
                doneInitialLoad = true
                isLoading = false
            }

            ImageGrid(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                navController,
                shouldShowLargeImage,
                initialPage,
                allImages
            ) {
                if (shouldKeepSearching) {
                    if (!isLoading) {
                        isLoading = true
                        scope.launch(Dispatchers.IO) {
                            val newImages = imageSource.loadPage(searchQuery, pageNumber)
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
    AnimatedVisibilityLargeImageView(shouldShowLargeImage, navController, initialPage, allImages)
}
