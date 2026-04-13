package moe.apex.breadboard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.apex.breadboard.image.Image
import moe.apex.breadboard.image.ImageBoardAuth
import moe.apex.breadboard.image.AI_TAG_NAMES
import moe.apex.breadboard.image.ImageRating
import moe.apex.breadboard.preferences.ImageSource


class SearchResultsViewModel : ViewModel(), GridStateHolder by GridStateHolderDelegate() {
    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val _doneInitialLoad = MutableStateFlow(false)
    val doneInitialLoad = _doneInitialLoad.asStateFlow()

    private val _auth = MutableStateFlow<ImageBoardAuth?>(null)
    val auth = _auth.asStateFlow()

    private val _images = MutableStateFlow<List<Image>>(emptyList())
    val images = _images.asStateFlow()

    private val _shouldKeepSearching = MutableStateFlow(true)

    private val _pageNumber = MutableStateFlow(0)

    private val _blockedTags = MutableStateFlow<Set<String>>(emptySet())
    val blockedTags = _blockedTags.asStateFlow()

    private lateinit var imageSource: ImageSource
    private lateinit var query: String
    private var tagList: List<String> = emptyList()


    fun setup(
        imageSource: ImageSource,
        auth: ImageBoardAuth?,
        tags: List<String>
    ) {
        Log.i("SearchResults", "Setting up SearchResultsViewModel with source: ${imageSource.name}, tags: $tags")
        this.imageSource = imageSource
        tagList = tags
        query = imageSource.imageBoard.formatTagNameString(tags)
        _auth.value = auth
        _pageNumber.value = imageSource.imageBoard.firstPageIndex
        resetGridStates()
        _isReady.value = true
    }


    fun prepareReset() {
        Log.i("SearchResults", "Resetting SearchResultsViewModel")
        _isReady.value = false
    }


    fun updateAuth(auth: ImageBoardAuth?) {
        _auth.value = auth
    }


    fun updateBlockedTags(manuallyBlockedTags: Set<String>, blockAi: Boolean) {
        val blockList = if (AI_TAG_NAMES.any { it in tagList }) {
            // Even if blockAi is true, we'll leave them unblocked if the user explicitly searched for AI
            manuallyBlockedTags
        } else if (blockAi) {
            manuallyBlockedTags + AI_TAG_NAMES
        } else {
            manuallyBlockedTags
        }
        _blockedTags.value = blockList.filter { it !in tagList }.toSet()
    }


    suspend fun loadMore() {
        if (!_shouldKeepSearching.value) {
            Log.i("SearchResults", "No more images to load, stopping search.")
            return
        }
        if (!_isReady.value) {
            throw IllegalStateException("SearchResultsViewModel is not ready. Call setup() first.")
        }

        try {
            Log.i("SearchResults", "Loading more images for query: $query, page: ${_pageNumber.value}")
            val newImages = imageSource.imageBoard.loadPage(query, _pageNumber.value, auth.value)
            if (newImages.isEmpty()) {
                _shouldKeepSearching.value = false
            } else {
                if (_pageNumber.value == imageSource.imageBoard.firstPageIndex) {
                    _images.value = newImages
                } else {
                    _images.value += newImages.filter { it !in _images.value }
                }
                _pageNumber.value++
            }
        } catch (e: Exception) {
            Log.e("SearchResults", "Error loading more images", e)
            _shouldKeepSearching.value = false
        }
        if (!_doneInitialLoad.value) {
            _doneInitialLoad.value = true
        }
    }


    fun updateImage(oldImage: Image, newImage: Image) {
        val index = _images.value.indexOf(oldImage)
        if (index != -1) {
            val updatedImages = _images.value.toMutableList().apply { this[index] = newImage }
            _images.value = updatedImages
        }
    }


    fun filterImages(ratings: Set<ImageRating>? = null): List<Image> {
        return _images.value.filter { image ->
            val isNotBlocked = image.metadata!!.tags.none { tag ->
                _blockedTags.value.contains(tag.lowercase())
            }
            val passesRatingFilter = ratings?.contains(image.metadata.rating) ?: true

            isNotBlocked && passesRatingFilter
        }
    }
}