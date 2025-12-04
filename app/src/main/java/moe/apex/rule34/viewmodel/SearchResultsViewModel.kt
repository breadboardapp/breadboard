package moe.apex.rule34.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageBoardAuth
import moe.apex.rule34.preferences.ImageSource


class SearchResultsViewModel : ViewModel(), GridStateHolder by GridStateHolderDelegate() {
    private var isReadyInternal by mutableStateOf(false)
    var doneInitialLoad by mutableStateOf(false)
    var auth: ImageBoardAuth? = null
    val images = mutableStateListOf<Image>()
    private var shouldKeepSearching by mutableStateOf(true)
    private var pageNumber by mutableIntStateOf(0) // We'll set this to the proper value later
    private lateinit var imageSource: ImageSource
    private lateinit var query: String
    var isReady: Boolean = isReadyInternal
        private set


    fun setup(imageSource: ImageSource, auth: ImageBoardAuth?, tags: List<String>) {
        if (isReady) {
            return
        }
        val authProvided = auth != null
        Log.i("SearchResults", "Setting up SearchResultsViewModel with source: ${imageSource.name}, authenticated: $authProvided, tags: $tags")
        this.imageSource = imageSource
        this.auth = auth
        query = imageSource.imageBoard.formatTagNameString(tags)
        pageNumber = imageSource.imageBoard.firstPageIndex
        resetGridStates()
        isReady = true
    }


    fun prepareReset() {
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
                        Snapshot.withMutableSnapshot {
                            images.clear()
                            images.addAll(newImages)
                        }
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