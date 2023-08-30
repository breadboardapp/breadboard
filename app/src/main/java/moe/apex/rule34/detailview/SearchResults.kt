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

/*
class ResultsPagingSource(private val imageSource: ImageSource) : PagingSource<Int, Image>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Image> {
        val page: Int = params.key!!
        val images = imageSource.loadPage(page)
        Log.i("page", "$page")
        Log.i("number of results", "${images.size}")
        return LoadResult.Page(
            data = images,
            prevKey = null,
            nextKey = page + 1
        )
    }


    override fun getRefreshKey(state: PagingState<Int, Image>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}


class ImageViewModel(tags: String) : ViewModel() {
    val imageSource = ImageSource(tags)
    val resultsPagingSource = ResultsPagingSource(imageSource)
    val flow = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = 60),
        initialKey = 0
    ) {
        resultsPagingSource
    }.flow
        .cachedIn(viewModelScope)

    //init {
    //    viewModelScope.launch {
    //        resultsPagingSource.load(PagingSource.LoadParams.Append(0, 60, false))
    //    }
    //}
}
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResults(navController: NavController, searchQuery: String) {
    // val viewModel = ImageViewModel(searchQuery)
    val topAppBarScrollState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarScrollState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    // var currentHdImageUrl = remember { mutableStateOf("") }
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
                    // currentHdImageUrl,
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
            // currentHdImageUrl.value,
            initialPage,
            shouldShowLargeImage,
            allImages
        )
    }
}
