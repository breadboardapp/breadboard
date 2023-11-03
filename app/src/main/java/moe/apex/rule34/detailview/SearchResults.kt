package moe.apex.rule34.detailview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageSource
import moe.apex.rule34.largeimageview.LargeImageView
import moe.apex.rule34.ui.theme.ProcrasturbatingTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResults(navController: NavController, searchQuery: String) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    val initialPage = remember { mutableIntStateOf(0) }
    val allImages = remember { mutableStateListOf<Image>() }

    ProcrasturbatingTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                LargeTopAppBar(
                    title = { Text("Search results") },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigateUp() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Home"
                            )
                        }
                    }
                )
            }
        ) {
            Column(
                Modifier
                    .padding(it)
                    .padding(horizontal = 16.dp)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {

                ImageGrid(
                    ImageSource(searchQuery),
                    navController,
                    shouldShowLargeImage,
                    initialPage,
                    allImages
                )
            }
        }
    }
    AnimatedVisibility(
        visible = shouldShowLargeImage.value,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY =  { it })
    ) {
        LargeImageView(
            navController,
            initialPage,
            shouldShowLargeImage,
            allImages
        )
    }
}
