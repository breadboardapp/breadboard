package moe.apex.rule34.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import moe.apex.rule34.image.Image
import moe.apex.rule34.tag.TagSuggestion
import moe.apex.rule34.util.RecommendationsProvider


object GlobalViewModelOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}


class BreadboardViewModel : ViewModel() {
    private val _tagSuggestions = MutableStateFlow<List<TagSuggestion>>(emptyList())
    val tagSuggestions: StateFlow<List<TagSuggestion>> = _tagSuggestions.asStateFlow()

    private val _recommendationsProvider = MutableStateFlow<RecommendationsProvider?>(null)
    val recommendationsProvider: StateFlow<RecommendationsProvider?> = _recommendationsProvider.asStateFlow()

    private val _downloadingImages = MutableStateFlow<Set<Image>>(emptySet())
    val downloadingImages: StateFlow<Set<Image>> = _downloadingImages.asStateFlow()

    private val _incognito = MutableStateFlow(false)
    val incognito: StateFlow<Boolean> = _incognito.asStateFlow()

    private val _userMutePreference = MutableStateFlow<Boolean?>(null)
    val userMutePreference: StateFlow<Boolean?> = _userMutePreference.asStateFlow()

    fun setRecommendationsProvider(provider: RecommendationsProvider?) {
        _recommendationsProvider.value = provider
    }

    fun setIncognito(value: Boolean) {
        _incognito.value = value
    }

    fun setUserMutePreference(muted: Boolean) {
        _userMutePreference.value = muted
    }

    fun addTagSuggestion(tag: TagSuggestion) {
        _tagSuggestions.update { current ->
            current + tag
        }
    }

    fun replaceTagSuggestion(index: Int, tag: TagSuggestion) {
        _tagSuggestions.update { current ->
            current.toMutableList().apply {
                set(index, tag)
            }
        }
    }

    fun setTagSuggestions(tags: List<TagSuggestion>) {
        _tagSuggestions.update { tags }
    }

    fun removeTagSuggestion(tag: TagSuggestion) {
        _tagSuggestions.update { current ->
            current.filterNot { it.value == tag.value }
        }
    }

    fun clearTagSuggestions() {
        _tagSuggestions.update { emptyList() }
    }

    fun addDownloadingImage(image: Image) {
        _downloadingImages.update { it + image }
    }

    fun removeDownloadingImage(image: Image) {
        _downloadingImages.update { it - image }
    }
}


fun List<TagSuggestion>.getIndexByName(name: String): Int? {
    val index = indexOfFirst { it.value == name }
    return if (index == -1) null else index
}
