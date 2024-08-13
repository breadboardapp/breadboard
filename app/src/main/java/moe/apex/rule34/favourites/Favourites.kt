package moe.apex.rule34.favourites

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.apex.rule34.detailview.ImageGrid
import moe.apex.rule34.preferences.Prefs
import moe.apex.rule34.prefs
import moe.apex.rule34.util.AnimatedVisibilityLargeImageView
import moe.apex.rule34.util.TitleBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesPage(navController: NavController) {
    val context = LocalContext.current
    val prefs by context.prefs.getPreferences.collectAsState(initial = Prefs.DEFAULT)
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    val initialPage = remember { mutableIntStateOf(0) }

    val images = prefs.favouriteImages.reversed()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TitleBar("Favourite images", scrollBehavior, navController)
        }
    ) { padding ->
        ImageGrid(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            navController = navController,
            shouldShowLargeImage = shouldShowLargeImage,
            initialPage = initialPage,
            images = images
        )
    }
    AnimatedVisibilityLargeImageView(shouldShowLargeImage, navController, initialPage, images)
}
