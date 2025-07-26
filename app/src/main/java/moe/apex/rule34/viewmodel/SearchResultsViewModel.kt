package moe.apex.rule34.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageBoardAuth
import moe.apex.rule34.preferences.ImageSource


@SuppressLint("MutableCollectionMutableState")
class SearchResultsViewModel : ViewModel() {
    var isReady by mutableStateOf(false)
    var doneInitialLoad by mutableStateOf(false)
    // Not great but it avoids the momentary period where the list is empty when doing a new search.
    var images by mutableStateOf(mutableStateListOf<Image>())
    lateinit var uniformGridState: LazyGridState
    lateinit var staggeredGridState: LazyStaggeredGridState
    private var shouldKeepSearching by mutableStateOf(true)
    private var pageNumber by mutableIntStateOf(0) // We'll set this to the proper value later
    private var auth: ImageBoardAuth? = null
    private lateinit var imageSource: ImageSource
    private lateinit var query: String


    fun setup(imageSource: ImageSource, auth: ImageBoardAuth?, tags: List<String>) {
        if (isReady) {
            return
        }
        Log.i("SearchResults", "Setting up SearchResultsViewModel with source: ${imageSource.name}, auth: $auth, tags: $tags")
        this.imageSource = imageSource
        this.auth = auth
        query = imageSource.imageBoard.formatTagNameString(tags)
        pageNumber = imageSource.imageBoard.firstPageIndex
        uniformGridState = LazyGridState()
        staggeredGridState = LazyStaggeredGridState()
        isReady = true
    }


    fun reset() {
        Log.i("SearchResults", "Resetting SearchResultsViewModel")
        isReady = false
    }


    suspend fun loadMore() {
        if (!shouldKeepSearching) {
            Log.i("SearchResults", "No more images to load, stopping search.")
            return
        }
        if (!isReady) {
            throw IllegalStateException("SearchResultsViewModel is not ready. Call setup() first.")
        }
        withContext(Dispatchers.IO) {
            try {
                Log.i("SearchResults", "Loading more images for query: $query, page: $pageNumber")
                val newImages = imageSource.imageBoard.loadPage(query, pageNumber, auth)
                if (newImages.isEmpty()) {
                    shouldKeepSearching = false
                } else {
                    if (pageNumber == imageSource.imageBoard.firstPageIndex) {
                        images = newImages.toMutableStateList()
                    } else {
                        images += newImages.filter { it !in images }
                    }
                    pageNumber++
                }
            } catch (e: Exception) {
                Log.e("SearchResults", "Error loading more images", e)
                shouldKeepSearching = false
            }
            if (!doneInitialLoad) {
                doneInitialLoad = true
            }
        }
    }
}