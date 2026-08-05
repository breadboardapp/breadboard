package moe.apex.breadboard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.apex.breadboard.image.Image
import moe.apex.breadboard.image.ImageBoardAuth
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.saucenao.SauceNao
import moe.apex.breadboard.saucenao.SauceNaoException
import moe.apex.breadboard.saucenao.SauceNaoResponse
import moe.apex.breadboard.saucenao.SauceNaoResponseHeader
import moe.apex.breadboard.saucenao.SauceNaoResultGroup
import moe.apex.breadboard.saucenao.groupResults


sealed class ResultsState {
    data object Loading : ResultsState()
    data class Success(val header: SauceNaoResponseHeader, val groups: List<SauceNaoResultGroup>) : ResultsState()
    data class Error(val message: String, val statusCode: Int?) : ResultsState()
}


class SaucenaoResultsViewModel: ViewModel() {
    private val _resultsState = MutableStateFlow<ResultsState>(ResultsState.Loading)
    val resultsState = _resultsState.asStateFlow()
    private val _viewableImages = MutableStateFlow<List<Image>>(emptyList())
    val viewableImages = _viewableImages.asStateFlow()


    suspend fun performSearch(
        imageUrl: String,
        apiKey: String,
        allowNsfw: Boolean,
        authFactory: (ImageSource) -> ImageBoardAuth?
    ) {
        _resultsState.value = ResultsState.Loading
        try {
            val response = SauceNao.search(imageUrl, apiKey, allowNsfw = allowNsfw)
            processResponse(response, authFactory)
        } catch (e: SauceNaoException) {
            _resultsState.value = ResultsState.Error(e.message ?: "Unknown SauceNAO error", e.statusCode)
        } catch (e: Exception) {
            Log.e("SauceNAO", "Error searching SauceNAO", e)
            _resultsState.value = ResultsState.Error(e.message ?: "An error occurred", null)
        }
    }

    suspend fun performFileSearch(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        apiKey: String,
        allowNsfw: Boolean,
        authFactory: (ImageSource) -> ImageBoardAuth?
    ) {
        _resultsState.value = ResultsState.Loading
        try {
            val response = SauceNao.searchWithFile(fileBytes, fileName, mimeType, apiKey, allowNsfw = allowNsfw)
            processResponse(response, authFactory)
        } catch (e: SauceNaoException) {
            _resultsState.value = ResultsState.Error(e.message ?: "Unknown SauceNAO error", e.statusCode)
        } catch (e: Exception) {
            Log.e("SauceNAO", "Error searching SauceNAO", e)
            _resultsState.value = ResultsState.Error(e.message ?: "An error occurred", null)
        }
    }

    private suspend fun processResponse(
        response: SauceNaoResponse,
        authFactory: (ImageSource) -> ImageBoardAuth?
    ) {
        val results = response.results.sortedByDescending { it.header.similarity }
        val groups = groupResults(results)
        _resultsState.value = ResultsState.Success(response.header, groups)

        // Preload Image objects for all results across all groups
        for (group in groups) {
            val allResults = listOf(group.primaryResult) + group.relatedResults
            for (result in allResults) {
                val imageViews = result.data.parseImageBoards()
                imageViews.forEach {
                    try {
                        val auth = authFactory(it.source)
                        val image = it.source.imageBoard.loadImage(it.id, auth)
                        if (image != null) {
                            if (image.hasGroupedTags) {
                                _viewableImages.value += image
                            } else {
                                val meta = it.source.imageBoard.loadImageGroupedTags(image, auth)
                                _viewableImages.value += image.copy(metadata = meta)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(
                            "SauceNAO",
                            "Failed to load image for ${it.source}:${it.id}",
                            e
                        )
                    }
                }
            }
        }
    }
}