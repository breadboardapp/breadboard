package moe.apex.breadboard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.apex.breadboard.artist.Artist
import moe.apex.breadboard.image.Danbooru
import moe.apex.breadboard.image.DanbooruSafe
import moe.apex.breadboard.image.Image


class ArtistProfileViewModel(private val artistTag: String): ViewModel() {
    private val _artistProfile = MutableStateFlow<Artist?>(null)
    val artistProfile = _artistProfile.asStateFlow()
    private val _isInitialised = MutableStateFlow(false)
    val isInitialised = _isInitialised.asStateFlow()

    private val _images = MutableStateFlow<List<Image>>(emptyList())
    val images = _images.asStateFlow()


    suspend fun loadArtistProfile() {
        if (_isInitialised.value) {
            return
        }

        Log.i("ArtistProfileViewModel", "Loading artist profile for $artistTag")

        _artistProfile.value = runCatching { Danbooru.getArtist(artistTag) }
            .onFailure { Log.e("ArtistProfileViewModel", "Failed to load artist profile for $artistTag", it) }
            .getOrThrow()

        runCatching { DanbooruSafe.getMostPopularPosts(artistTag) }
            .onFailure { Log.e("ArtistProfileViewModel", "Failed to load posts with DanbooruSafe for artist $artistTag. Attempting regular Danbooru.", it) }
            .recoverCatching { Danbooru.getMostPopularPosts(artistTag) }
            .onFailure { Log.e("ArtistProfileViewModel", "Failed to load posts with Danbooru for artist $artistTag", it) }
            .onSuccess { _images.value = it }

        _isInitialised.value = true
    }
}
