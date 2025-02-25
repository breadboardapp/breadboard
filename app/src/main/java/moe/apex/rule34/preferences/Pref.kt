package moe.apex.rule34.preferences

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import moe.apex.rule34.history.SearchHistoryEntry
import moe.apex.rule34.history.encodeToByteArray
import moe.apex.rule34.image.Danbooru
import moe.apex.rule34.image.Gelbooru
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageBoard
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.image.Rule34
import moe.apex.rule34.image.Safebooru
import moe.apex.rule34.image.Yandere
import moe.apex.rule34.navigation.ImageView
import moe.apex.rule34.tag.TagCategory
import moe.apex.rule34.util.availableRatingsForSource
import moe.apex.rule34.util.extractPixivId
import java.io.IOException


val LocalPreferences = compositionLocalOf {
    Prefs.DEFAULT
}


interface PrefEnum<T : Enum<T>> {
    val description: String
}


data object PrefNames {
    const val DATA_SAVER = "data_saver"
    const val STORAGE_LOCATION = "storage_location"
    const val FAVOURITE_IMAGES = "favourite_images"
    const val EXCLUDE_AI = "exclude_ai"
    const val IMAGE_SOURCE = "image_source"
    const val FAVOURITES_FILTER = "favourites_filter"
    const val LAST_USED_VERSION_CODE = "last_used_version_code"
    const val RATINGS_FILTER = "ratings_filter"
    const val FAVOURITES_RATING_FILTER = "favourites_rating_filter"
    const val FILTER_RATINGS_LOCALLY = "filter_ratings_locally"
    const val USE_STAGGERED_GRID = "use_staggered_grid"
    const val SAVE_SEARCH_HISTORY = "save_search_history"
    const val SEARCH_HISTORY = "search_history"
}


enum class DataSaver(override val description: String) : PrefEnum<DataSaver> {
    ON ("Always"),
    OFF ("Never"),
    AUTO ("When using mobile data")
}


data class Prefs(
    val dataSaver: DataSaver,
    val storageLocation: Uri,
    val favouriteImages: List<Image>,
    val excludeAi: Boolean,
    val imageSource: ImageSource,
    val favouritesFilter: List<ImageSource>,
    val lastUsedVersionCode: Int,
    val ratingsFilter: List<ImageRating>,
    val favouritesRatingsFilter: List<ImageRating>,
    val filterRatingsLocally: Boolean,
    val useStaggeredGrid: Boolean,
    val saveSearchHistory: Boolean,
    val searchHistory: List<SearchHistoryEntry>
) {
    companion object {
        val DEFAULT = Prefs(
            dataSaver = DataSaver.AUTO,
            storageLocation = Uri.EMPTY,
            favouriteImages = emptyList(),
            excludeAi = false,
            imageSource = ImageSource.SAFEBOORU,
            favouritesFilter = ImageSource.entries.toList(),
            lastUsedVersionCode = 0, // We'll update this later
            ratingsFilter = listOf(ImageRating.SAFE),
            favouritesRatingsFilter = listOf(ImageRating.SAFE),
            filterRatingsLocally = true,
            useStaggeredGrid = false,
            saveSearchHistory = true,
            searchHistory = emptyList(),
        )
    }
}


class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private object PreferenceKeys {
        val DATA_SAVER = stringPreferencesKey(PrefNames.DATA_SAVER)
        val STORAGE_LOCATION = stringPreferencesKey(PrefNames.STORAGE_LOCATION)
        val FAVOURITE_IMAGES = byteArrayPreferencesKey(PrefNames.FAVOURITE_IMAGES)
        val EXCLUDE_AI = booleanPreferencesKey(PrefNames.EXCLUDE_AI)
        val IMAGE_SOURCE = stringPreferencesKey(PrefNames.IMAGE_SOURCE)
        val FAVOURITES_FILTER = stringSetPreferencesKey(PrefNames.FAVOURITES_FILTER)
        val LAST_USED_VERSION_CODE = intPreferencesKey(PrefNames.LAST_USED_VERSION_CODE)
        val RATINGS_FILTER = stringSetPreferencesKey(PrefNames.RATINGS_FILTER)
        val FAVOURITES_RATING_FILTER = stringSetPreferencesKey(PrefNames.FAVOURITES_RATING_FILTER)
        val FILTER_RATINGS_LOCALLY = booleanPreferencesKey(PrefNames.FILTER_RATINGS_LOCALLY)
        val USE_STAGGERED_GRID = booleanPreferencesKey(PrefNames.USE_STAGGERED_GRID)
        val SAVE_SEARCH_HISTORY = booleanPreferencesKey(PrefNames.SAVE_SEARCH_HISTORY)
        val SEARCH_HISTORY = byteArrayPreferencesKey(PrefNames.SEARCH_HISTORY)
    }

    val getPreferences: Flow<Prefs> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                Log.e("preferences", "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            mapUserPreferences(preferences)
        }

    @Suppress("DEPRECATION")
    suspend fun handleMigration(context: Context) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val isOnFirstInstallVersion = packageInfo.firstInstallTime == packageInfo.lastUpdateTime

        val currentPreferences = dataStore.data.first()
        val lastUsedVersionCode = currentPreferences[PreferenceKeys.LAST_USED_VERSION_CODE] ?: 0

        if (isOnFirstInstallVersion)
            return updateLastUsedVersionCode(packageInfo)

        /* Version code 230 introduced app version tracking and also changed the default source from
           R34 to Safebooru but we don't want to change it for existing users.
           If the last used version code is below 230 (always 0 as we didn't save it before 230)
           and isFirstRun is false then it means the user has updated the app. */
        if (lastUsedVersionCode < 230)
            updateImageSource(ImageSource.R34)

        /* Version code 240 introduced the ratings filter. Keep all ratings enabled for existing
           users. New users will get the default set of ratings (only SAFE). */
        if (lastUsedVersionCode < 240) {
            val validSearchRatings = ImageRating.entries.filter { it != ImageRating.UNKNOWN }.toSet()
            updateRatingsFilter(validSearchRatings)
            updateFavouritesRatingFilter(ImageRating.entries.toSet())
        }

        /* Version code 251 introduced grouped tags, which use the new groupedTags property of ImageMetadata.
           Additionally, it made the Pixiv ID extractor account for posts with multiple images.
           Move old tags of existing users' favourites to the new grouped tags.
           Also remove older copies of duplicate images and try to extract Pixiv ID again. */
        if (lastUsedVersionCode < 251) {
            val images = mutableListOf<Image>()

            for (image in getPreferences.first().favouriteImages) {
                val existing = images.find { it.fileName == image.fileName && it.imageSource == image.imageSource }
                if (existing != null) {
                    /* As favouriteImages stores favourites in the order they were added, we can
                       safely remove the older duplicate as it will have the out-of-date metadata. */
                    images.remove(existing)
                }

                images.add(
                    image.copy(
                        metadata = image.metadata?.copy(
                            tags = null,
                            groupedTags = if (!image.metadata.tags.isNullOrEmpty()) listOf(TagCategory.GENERAL.group(image.metadata.tags))
                                          else emptyList(),
                            pixivId = image.metadata.pixivId ?: extractPixivId(image.metadata.source)
                        )
                    )
                )
            }

            updateFavouriteImages(images)
        }

        /* Version code 251 updated the local filter setting (introduced in version 240) to be
           enabled by default.
           However, we don't want to change it for anyone who had access to the setting previously
           and never toggled it. */
        if (lastUsedVersionCode in 240 .. 250) {
            val data = dataStore.data.first()
            val filterRatingsLocally = data[PreferenceKeys.FILTER_RATINGS_LOCALLY]
            if (filterRatingsLocally == null)
                updateFilterRatingsLocally(false)
        }

        // Place any future migrations above this line by checking the last used version code.
        if (getCurrentRunningVersionCode(packageInfo) >= lastUsedVersionCode)
            updateLastUsedVersionCode(packageInfo)
    }

    suspend fun updateDataSaver(to: DataSaver) {
        // updateData handles data transactionally, ensuring that if the sort is updated at the same
        // time from another thread, we won't have conflicts
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DATA_SAVER] = to.name
        }
    }

    suspend fun updateStorageLocation(to: Uri) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.STORAGE_LOCATION] = to.toString()
        }
    }

    private suspend fun updateFavouriteImages(to: List<Image>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FAVOURITE_IMAGES] = to.encodeToByteArray()
        }
    }

    suspend fun addFavouriteImage(image: Image) {
        val images = getPreferences.first().favouriteImages.toMutableList().apply { add(image) }
        updateFavouriteImages(images)
    }

    suspend fun removeFavouriteImage(image: Image) {
        val images = getPreferences.first().favouriteImages.toMutableList().apply {
            removeAll { it.fileName == image.fileName }
        }
        updateFavouriteImages(images)
    }

    suspend fun updateExcludeAi(to: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.EXCLUDE_AI] = to
        }
    }

    suspend fun updateImageSource(to: ImageSource) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IMAGE_SOURCE] = to.name
        }
    }

    private suspend fun updateFavouritesFilter(to: Set<ImageSource>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FAVOURITES_FILTER] = to.mapTo(mutableSetOf()) { it.name }
        }
    }

    suspend fun addFavouritesFilter(source: ImageSource) {
        val sources = getPreferences.first().favouritesFilter.toMutableSet().apply { add(source) }
        updateFavouritesFilter(sources)
    }

    suspend fun removeFavouritesFilter(source: ImageSource) {
        val sources = getPreferences.first().favouritesFilter.toMutableSet().apply { remove(source) }
        updateFavouritesFilter(sources)
    }

    @Suppress("Deprecation")
    private fun getCurrentRunningVersionCode(packageInfo: PackageInfo): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode.toInt()
        else packageInfo.versionCode
    }

    private suspend fun updateLastUsedVersionCode(packageInfo: PackageInfo) {
        val versionCode = getCurrentRunningVersionCode(packageInfo)

        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_USED_VERSION_CODE] = versionCode
        }
    }

    suspend fun updateRatingsFilter(to: Set<ImageRating>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.RATINGS_FILTER] = to.mapTo(mutableSetOf()) { it.name }
        }
    }

    suspend fun addRatingFilter(rating: ImageRating) {
        val ratings = getPreferences.first().ratingsFilter.toMutableSet().apply { add(rating) }
        updateRatingsFilter(ratings)
    }

    suspend fun removeRatingFilter(rating: ImageRating) {
        val ratings = getPreferences.first().ratingsFilter.toMutableSet().apply { remove(rating) }
        updateRatingsFilter(ratings)
    }

    private suspend fun updateFavouritesRatingFilter(to: Set<ImageRating>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FAVOURITES_RATING_FILTER] = to.mapTo(mutableSetOf()) { it.name }
        }
    }

    suspend fun addFavouritesRatingFilter(rating: ImageRating) {
        val ratings = getPreferences.first().favouritesRatingsFilter.toMutableSet().apply { add(rating) }
        updateFavouritesRatingFilter(ratings)
    }

    suspend fun removeFavouritesRatingFilter(rating: ImageRating) {
        val ratings = getPreferences.first().favouritesRatingsFilter.toMutableSet().apply { remove(rating) }
        updateFavouritesRatingFilter(ratings)
    }

    suspend fun updateFilterRatingsLocally(to: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FILTER_RATINGS_LOCALLY] = to
        }
    }

    suspend fun updateUseStaggeredGrid(to: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.USE_STAGGERED_GRID] = to
        }
    }

    suspend fun updateSaveSearchHistory(to: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SAVE_SEARCH_HISTORY] = to
        }
    }

    private suspend fun updateSearchHistory(to: List<SearchHistoryEntry>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SEARCH_HISTORY] = to.encodeToByteArray()
        }
    }

    private fun findDuplicate(incoming: SearchHistoryEntry, history: List<SearchHistoryEntry>): SearchHistoryEntry? {
        /* Since not all ratings are available for all sources, we should only check the
           ones that are available when determining whether a search history entry is a duplicate.

           Additionally, check the formatted labels of tags rather than the objects themselves
           as ones added from pasting will be missing metadata. */
        val validRatings = availableRatingsForSource(incoming.source)
        val incomingRatings = validRatings.filter { it in incoming.ratings }
        val incomingTagLabels = incoming.tags.mapTo(mutableSetOf()) { it.formattedLabel }

        for (existing in history) {
            if (incoming.source != existing.source) continue
            if (incomingRatings != validRatings.filter { it in existing.ratings }) continue

            if (incomingTagLabels == existing.tags.mapTo(mutableSetOf()) { it.formattedLabel } )
                return existing
        }
        return null
    }

    suspend fun addSearchHistoryEntry(entry: SearchHistoryEntry) {
        val history = getPreferences.first().searchHistory.toMutableList()
        val existing = findDuplicate(entry, history)

        history.apply {
            if (existing != null) remove(existing)
            if (size == 10) removeAt(0)
            add(entry)
        }

        updateSearchHistory(history)
    }


    suspend fun removeSearchHistoryEntry(entry: SearchHistoryEntry) {
        val history = getPreferences.first().searchHistory.toMutableList().apply {
            removeIf { it == entry }
        }
        updateSearchHistory(history)
    }

    suspend fun clearSearchHistory() {
        updateSearchHistory(emptyList())
    }


    @OptIn(ExperimentalSerializationApi::class)
    private fun mapUserPreferences(preferences: Preferences): Prefs {
        val dataSaver = DataSaver.valueOf(
            preferences[PreferenceKeys.DATA_SAVER] ?: DataSaver.AUTO.name
        )
        val storageLocation = Uri.parse(preferences[PreferenceKeys.STORAGE_LOCATION] ?: "")
        val favouriteImagesRaw = preferences[PreferenceKeys.FAVOURITE_IMAGES]
        val favouriteImages: List<Image> = favouriteImagesRaw?.let { Cbor.decodeFromByteArray(it) } ?: emptyList()
        val excludeAi = preferences[PreferenceKeys.EXCLUDE_AI] ?: false
        val imageSource = ImageSource.valueOf(preferences[PreferenceKeys.IMAGE_SOURCE] ?: ImageSource.SAFEBOORU.name)
        val favouritesFilter = (
            preferences[PreferenceKeys.FAVOURITES_FILTER]?.map { ImageSource.valueOf(it) } ?: ImageSource.entries
        )
        val lastUsedVersionCode = preferences[PreferenceKeys.LAST_USED_VERSION_CODE] ?: 0
        val ratingsFilter = (
            preferences[PreferenceKeys.RATINGS_FILTER]?.map { ImageRating.valueOf(it) } ?: listOf(ImageRating.SAFE)
        )
        val favouritesRatingFilter = (
            preferences[PreferenceKeys.FAVOURITES_RATING_FILTER]?.map { ImageRating.valueOf(it) } ?: listOf(ImageRating.SAFE)
        )
        val filterRatingsLocally = (
            preferences[PreferenceKeys.FILTER_RATINGS_LOCALLY] ?: true
        )
        val useStaggeredGrid = (
            preferences[PreferenceKeys.USE_STAGGERED_GRID] ?: false
        )
        val saveSearchHistory = (
            preferences[PreferenceKeys.SAVE_SEARCH_HISTORY] ?: true
        )
        val searchHistoryRaw = preferences[PreferenceKeys.SEARCH_HISTORY]
        val searchHistory: List<SearchHistoryEntry> = searchHistoryRaw?.let { Cbor.decodeFromByteArray(it) } ?: emptyList()

        return Prefs(
            dataSaver,
            storageLocation,
            favouriteImages,
            excludeAi,
            imageSource,
            favouritesFilter,
            lastUsedVersionCode,
            ratingsFilter,
            favouritesRatingFilter,
            filterRatingsLocally,
            useStaggeredGrid,
            saveSearchHistory,
            searchHistory
        )
    }

}


@OptIn(ExperimentalSerializationApi::class)
private fun List<Image>.encodeToByteArray(): ByteArray {
    return Cbor.encodeToByteArray(this)
}


enum class ImageSource(override val description: String, val site: ImageBoard) : PrefEnum<ImageSource> {
    SAFEBOORU("Safebooru", Safebooru),
    DANBOORU("Danbooru", Danbooru),
    GELBOORU("Gelbooru", Gelbooru),
    YANDERE("Yande.re", Yandere),
    R34("Rule34", Rule34);

    companion object {
        fun uriToImageView(uri: Uri?): ImageView? {
            if (uri == null) return null

            val imageSource = when (uri.host) {
                "safebooru.org" -> SAFEBOORU
                "danbooru.donmai.us" -> DANBOORU
                "gelbooru.com" -> GELBOORU
                "yande.re", "files.yande.re" -> YANDERE
                "rule34.xxx" -> R34
                else -> return null
            }

            val postId = when (imageSource) {
                SAFEBOORU,
                GELBOORU,
                R34 -> uri.getQueryParameter("id")
                DANBOORU -> uri.path?.split('/')?.getOrNull(2)
                YANDERE -> {
                    val postId = uri.path?.split('/')?.getOrNull(3)
                    if (uri.host == "files.yande.re") postId?.split(" ")?.getOrNull(1) else postId
                }
            } ?: return null

            return ImageView(imageSource, postId)
        }
    }
}