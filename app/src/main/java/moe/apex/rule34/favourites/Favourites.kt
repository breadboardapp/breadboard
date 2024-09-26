package moe.apex.rule34.favourites

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import moe.apex.rule34.detailview.ImageGrid
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.util.AnimatedVisibilityLargeImageView
import moe.apex.rule34.util.MainScreenScaffold


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesPage(bottomBarVisibleState: MutableState<Boolean>) {
    val prefs = LocalPreferences.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    val initialPage = remember { mutableIntStateOf(0) }

    val images = prefs.favouriteImages.reversed()

    MainScreenScaffold("Favourite images", scrollBehavior) { padding ->
        ImageGrid(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            shouldShowLargeImage = shouldShowLargeImage,
            initialPage = initialPage,
            showFilter = true,
            images = images
        )
    }
    AnimatedVisibilityLargeImageView(shouldShowLargeImage, initialPage, images, bottomBarVisibleState)
}
