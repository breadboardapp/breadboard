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

        _artistProfile.value = Danbooru.getArtist(artistTag)
        _images.value = DanbooruSafe.getMostPopularPosts(artistTag)

        _isInitialised.value = true
    }
}
