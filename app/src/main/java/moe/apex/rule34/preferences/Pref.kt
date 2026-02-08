package moe.apex.rule34.preferences

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
import moe.apex.rule34.BuildConfig
import moe.apex.rule34.history.SearchHistoryEntry
import moe.apex.rule34.image.Danbooru
import moe.apex.rule34.image.Gelbooru
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageBoard
import moe.apex.rule34.image.ImageBoardAuth
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.image.Rule34
import moe.apex.rule34.image.Safebooru
import moe.apex.rule34.image.Yandere
import moe.apex.rule34.tag.TagCategory
import moe.apex.rule34.util.AgeVerification
import moe.apex.rule34.util.MigrationOnlyField
import moe.apex.rule34.util.SecretsManager
import moe.apex.rule34.util.availableRatingsForSource
import moe.apex.rule34.util.extractPixivId
import moe.apex.rule34.util.decodeHtml
import java.io.IOException


val LocalPreferences = compositionLocalOf {
    Prefs.DEFAULT
}


interface PrefEnum<T : Enum<T>> {
    val label: String
    val enabledIcon: ImageVector?
        get() = null
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
    const val USE_FIXED_LINKS = "use_fixed_links"
    const val IMAGE_BOARD_AUTHS = "image_board_auths"
    const val MANUALLY_BLOCKED_TAGS = "manually_blocked_tags"
    const val IMAGE_VIEWER_ACTION_ORDER = "image_viewer_action_order"
    const val DEFAULT_START_DESTINATION = "default_start_destination"
    const val RECOMMEND_ALL_RATINGS = "recommend_all_ratings"
    const val ENABLED_EXPERIMENTS = "enabled_experiments"
    const val HAS_VERIFIED_AGE = "has_verified_age"
    const val FLAG_SECURE_MODE = "flag_secure_mode"
    const val UNFOLLOWED_TAGS = "unfollowed_tags"
    const val RECOMMENDATIONS_TAG_COUNT = "recommendations_tag_count"
    const val RECOMMENDATIONS_POOL_SIZE = "recommendations_pool_size"
    const val RECOMMENDATIONS_WEIGHTED_SELECTION = "recommendations_weighted_selection"
    const val INTERNAL_IGNORE_LIST_TIMESTAMP = "internal_ignore_list_timestamp"
    const val INTERNAL_IGNORE_LIST = "internal_ignore_list"
    const val AUTOPLAY_VIDEOS = "autoplay_videos"
}


object PreferenceKeys {
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
    val USE_FIXED_LINKS = booleanPreferencesKey(PrefNames.USE_FIXED_LINKS)
    val IMAGE_BOARD_AUTHS = byteArrayPreferencesKey(PrefNames.IMAGE_BOARD_AUTHS)
    val MANUALLY_BLOCKED_TAGS = stringSetPreferencesKey(PrefNames.MANUALLY_BLOCKED_TAGS)
    val IMAGE_VIEWER_ACTION_ORDER = stringPreferencesKey(PrefNames.IMAGE_VIEWER_ACTION_ORDER)
    val DEFAULT_START_DESTINATION = stringPreferencesKey(PrefNames.DEFAULT_START_DESTINATION)
    val RECOMMEND_ALL_RATINGS = booleanPreferencesKey(PrefNames.RECOMMEND_ALL_RATINGS)
    val ENABLED_EXPERIMENTS = stringSetPreferencesKey(PrefNames.ENABLED_EXPERIMENTS)
    val HAS_VERIFIED_AGE = booleanPreferencesKey(PrefNames.HAS_VERIFIED_AGE)
    val FLAG_SECURE_MODE = stringPreferencesKey(PrefNames.FLAG_SECURE_MODE)
    val UNFOLLOWED_TAGS = stringSetPreferencesKey(PrefNames.UNFOLLOWED_TAGS)
    val RECOMMENDATIONS_TAG_COUNT = intPreferencesKey(PrefNames.RECOMMENDATIONS_TAG_COUNT)
    val RECOMMENDATIONS_POOL_SIZE = intPreferencesKey(PrefNames.RECOMMENDATIONS_POOL_SIZE)
    val RECOMMENDATIONS_WEIGHTED_SELECTION = booleanPreferencesKey(PrefNames.RECOMMENDATIONS_WEIGHTED_SELECTION)
    val INTERNAL_IGNORE_LIST_TIMESTAMP = longPreferencesKey(PrefNames.INTERNAL_IGNORE_LIST_TIMESTAMP)
    val INTERNAL_IGNORE_LIST = stringSetPreferencesKey(PrefNames.INTERNAL_IGNORE_LIST)
    val AUTOPLAY_VIDEOS = stringPreferencesKey(PrefNames.AUTOPLAY_VIDEOS)
}


enum class PrefCategory(val label: String) {
    BUILD("Build"),
    SETTING("Settings"),
    FAVOURITE_IMAGES("Favourite images"),
    SEARCH_HISTORY("Search history")
}


data class PrefMeta(
    val category: PrefCategory,
    val exportable: Boolean = true,
    val mergeable: Boolean = false
)


enum class DataSaver(override val label: String) : PrefEnum<DataSaver> {
    ON("Always"),
    OFF("Never"),
    AUTO("When using mobile data")
}


enum class ToolbarAction(override val label: String, override val enabledIcon: ImageVector) : PrefEnum<ToolbarAction> {
    DOWNLOAD("Download", Icons.Rounded.Download),
    TOGGLE_HD("Toggle HD", Icons.Rounded.Hd),
    FAVOURITE("Favourite", Icons.Rounded.Favorite),
    SHARE("Share", Icons.Rounded.Share),
    INFO("About", Icons.Rounded.Info)
}


enum class StartDestination(override val label: String) : PrefEnum<StartDestination> {
    HOME("Browse"),
    SEARCH("Search"),
    FAVOURITES("Favourites")
}


enum class FlagSecureMode(override val label: String) : PrefEnum<FlagSecureMode> {
    ON("Always"),
    OFF("Never"),
    AUTO("When in Incognito mode")
}

enum class AutoplayVideosMode(override val label: String) : PrefEnum<AutoplayVideosMode> {
    ON("Always"),
    OFF("Never"),
    AUTO("When data saver is inactive")
}

enum class Experiment(override val label: String, val description: String? = null) : PrefEnum<Experiment> {
    ALWAYS_ANIMATE_SCROLL("Always animate scroll-to-top", "Enable smooth scrolling on all pages when using the scroll-to-top button."),
    IMMERSIVE_UI_EFFECTS("Immersive UI effects", "Enables immersive UI effects such as blur and translucency. Not all devices support this feature. Disable if you experience poor performance."),
    COPY_TO_CLIPBOARD("Copy images to clipboard", "Adds the ability to long-press the download button to copy images to the clipboard instead of saving them."),;


    @Composable
    fun isEnabled(): Boolean {
        val prefs = LocalPreferences.current
        return prefs.isExperimentEnabled(this)
    }
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
    val searchHistory: List<SearchHistoryEntry>,
    val useFixedLinks: Boolean,
    val imageBoardAuths: Map<ImageSource, ImageBoardAuth>,
    val manuallyBlockedTags: Set<String>,
    val imageViewerActions: List<ToolbarAction>,
    val defaultStartDestination: StartDestination,
    val recommendAllRatings: Boolean,
    val enabledExperiments: Set<Experiment>,
    private val hasVerifiedAge: Boolean,
    val flagSecureMode: FlagSecureMode,
    val unfollowedTags: Set<String>,
    val recommendationsTagCount: Int,
    val recommendationsPoolSize: Int,
    val recommendationsWeightedSelection: Boolean,
    val internalIgnoreListTimestamp: Long,
    val internalIgnoreList: Set<String>,
    val autoplayVideos: AutoplayVideosMode
) {
    companion object {
        val DEFAULT = Prefs(
            dataSaver = DataSaver.AUTO,
            storageLocation = Uri.EMPTY,
            favouriteImages = emptyList(),
            excludeAi = false,
            imageSource = ImageSource.SAFEBOORU,
            favouritesFilter = ImageSource.entries,
            lastUsedVersionCode = 0, // We'll update this later
            ratingsFilter = listOf(ImageRating.SAFE),
            favouritesRatingsFilter = listOf(ImageRating.SAFE),
            filterRatingsLocally = true,
            useStaggeredGrid = true,
            saveSearchHistory = true,
            searchHistory = emptyList(),
            useFixedLinks = false,
            imageBoardAuths = emptyMap(),
            manuallyBlockedTags = emptySet(),
            imageViewerActions = ToolbarAction.entries.toList(),
            defaultStartDestination = StartDestination.HOME,
            recommendAllRatings = false,
            enabledExperiments = emptySet(),
            hasVerifiedAge = false,
            flagSecureMode = FlagSecureMode.AUTO,
            unfollowedTags = emptySet(),
            recommendationsTagCount = 3,
            recommendationsPoolSize = 7,
            recommendationsWeightedSelection = true,
            internalIgnoreListTimestamp = 0,
            internalIgnoreList = emptySet(),
            autoplayVideos = AutoplayVideosMode.OFF
        )
    }


    /** Use [AgeVerification.hasVerifiedAge] instead. */
    fun getInternalAgeVerificationStatus(): Boolean {
        return hasVerifiedAge
    }


    fun authFor(source: ImageSource, context: Context): ImageBoardAuth? {
        if (source == ImageSource.R34) {
            return ImageBoardAuth(BuildConfig.R34_APP_ID, SecretsManager.getApiKey(context)!!)
        }
        return imageBoardAuths[source]
    }


    /** The users manually blocked tags, plus the AI tags if `excludeAi` is enabled. */
    val blockedTags: Set<String>
        get() = manuallyBlockedTags.toMutableSet().apply {
            if (excludeAi) addAll(ImageSource.entries.map { it.imageBoard.aiTagName })
        }


    fun isExperimentEnabled(experiment: Experiment): Boolean {
        return experiment in enabledExperiments
    }
}


class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val keyMetaMapping = mapOf(
            // I am sorry
            PreferenceKeys.DATA_SAVER to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.STORAGE_LOCATION to PrefMeta(PrefCategory.SETTING, exportable = false),
            PreferenceKeys.FAVOURITE_IMAGES to PrefMeta(PrefCategory.FAVOURITE_IMAGES, mergeable = true),
            PreferenceKeys.EXCLUDE_AI to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.IMAGE_SOURCE to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.FAVOURITES_FILTER to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.LAST_USED_VERSION_CODE to PrefMeta(PrefCategory.BUILD),
            PreferenceKeys.RATINGS_FILTER to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.FAVOURITES_RATING_FILTER to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.FILTER_RATINGS_LOCALLY to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.USE_STAGGERED_GRID to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.SAVE_SEARCH_HISTORY to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.SEARCH_HISTORY to PrefMeta(PrefCategory.SEARCH_HISTORY),
            PreferenceKeys.USE_FIXED_LINKS to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.IMAGE_BOARD_AUTHS to PrefMeta(PrefCategory.SETTING, exportable = false),
            PreferenceKeys.MANUALLY_BLOCKED_TAGS to PrefMeta(PrefCategory.SETTING, mergeable = true),
            PreferenceKeys.IMAGE_VIEWER_ACTION_ORDER to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.DEFAULT_START_DESTINATION to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.RECOMMEND_ALL_RATINGS to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.ENABLED_EXPERIMENTS to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.HAS_VERIFIED_AGE to PrefMeta(PrefCategory.SETTING, exportable = false),
            PreferenceKeys.FLAG_SECURE_MODE to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.UNFOLLOWED_TAGS to PrefMeta(PrefCategory.SETTING, mergeable = true),
            PreferenceKeys.RECOMMENDATIONS_TAG_COUNT to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.RECOMMENDATIONS_POOL_SIZE to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.RECOMMENDATIONS_WEIGHTED_SELECTION to PrefMeta(PrefCategory.SETTING),
            PreferenceKeys.INTERNAL_IGNORE_LIST_TIMESTAMP to PrefMeta(PrefCategory.SETTING, exportable = false),
            PreferenceKeys.INTERNAL_IGNORE_LIST to PrefMeta(PrefCategory.SETTING, exportable = false)
        )
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


    fun ensureNotMalformed(target: Preferences): Boolean {
        try {
            mapUserPreferences(target)
            return true
        } catch (e: Exception) {
            Log.e("preferences", "Malformed preferences data.", e)
            return false
        }
    }


    @OptIn(ExperimentalSerializationApi::class, MigrationOnlyField::class)
    @Suppress("DEPRECATION")
    suspend fun handleMigration(context: Context) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val isOnFirstInstallVersion = packageInfo.firstInstallTime == packageInfo.lastUpdateTime

        val currentPreferences = dataStore.data.first()
        val lastUsedVersionCode = currentPreferences[PreferenceKeys.LAST_USED_VERSION_CODE] ?: 0

        /* lastUsedVersionCode can be 0 if the user had it installed already but cleared the data.
           In such a case, we can't reliably determine what their previous version was. Just load
           the default settings. */
        if (isOnFirstInstallVersion || lastUsedVersionCode == 0)
            return updateLastUsedVersionCode()

        /* As of version code 270, the migration to keep users on the R34 source if they updated
           from older than 230 was removed. Users updating from below 230 straight to 270+ will have
           their source reset to Safebooru and rating set to Safe, so they will no longer see NSFW
           content without adjusting their settings. */

        /* Version code 240 introduced the ratings filter. Keep all ratings enabled for existing
           users. New users will get the default set of ratings (only SAFE). */
        if (lastUsedVersionCode < 240) {
            val validSearchRatings = ImageRating.entries.filter { it != ImageRating.UNKNOWN }
            updateSet(PreferenceKeys.RATINGS_FILTER, validSearchRatings.map { it.name })
            updateSet(PreferenceKeys.FAVOURITES_RATING_FILTER, validSearchRatings.map { it.name })
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
                            uncategorisedTags = null,
                            groupedTags = if (!image.metadata.uncategorisedTags.isNullOrEmpty()) listOf(TagCategory.GENERAL.group(image.metadata.tags))
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
                updatePref(PreferenceKeys.FILTER_RATINGS_LOCALLY, false)
        }

        /* Version code 270 enabled the staggered grid by default. Don't change it for people who
           hadn't enabled it previously.
           Additionally, fix Gelbooru favourite image links since their subdomain changed from img3
           to img4. */
        if (lastUsedVersionCode < 270) {
            val data = dataStore.data.first()
            val brokenFavouritesByteArray = data[PreferenceKeys.FAVOURITE_IMAGES]

            if (brokenFavouritesByteArray != null) {
                val brokenFavourites: List<Image> = Cbor.decodeFromByteArray(brokenFavouritesByteArray)
                val tempFavourites = brokenFavourites.toMutableList()
                brokenFavourites.forEachIndexed { index, img ->
                    if (img.imageSource == ImageSource.GELBOORU) {
                        val fixedImage = img.copy(
                            previewUrl = img.previewUrl.replace("img3.gelbooru", "img4.gelbooru"),
                            fileUrl = img.fileUrl.replace("img3.gelbooru", "img4.gelbooru"),
                            sampleUrl = img.sampleUrl.replace("img3.gelbooru", "img4.gelbooru")
                        )
                        tempFavourites[index] = fixedImage
                    }
                }
                updateFavouriteImages(tempFavourites)
            }
        }

        // v3 (code 300) migrations start here

        /* Versions 270 and 271 had a bug where staggered might still be disabled by default for new
           installs. We'll keep it disabled for people affected by this but it will be enabled by
           default for new installations of v3.

           v3 supports having multiple artists for an image. Migrate existing favourites by splitting
           the artist string by a space. */
        if (lastUsedVersionCode < 300) {
            val data = dataStore.data.first()
            val useStaggeredGrid = data[PreferenceKeys.USE_STAGGERED_GRID]
            if (useStaggeredGrid == null)
                updatePref(PreferenceKeys.USE_STAGGERED_GRID, false)

            val favImagesByteArray = data[PreferenceKeys.FAVOURITE_IMAGES]
            if (favImagesByteArray != null) {
                val favImages: List<Image> = Cbor.decodeFromByteArray(favImagesByteArray)
                val migrated = favImages.map { image ->
                    val meta = image.metadata
                    if (meta != null) {
                        image.copy(
                            metadata = meta.copy(
                                artistsString = null,
                                artists = meta.artistsString?.split(" ") ?: emptyList()
                            )
                        )
                    } else image
                }
                updateFavouriteImages(migrated)
            }
        }

        /* Version 3.0.5 added support for specifying a key for R34 as unauthenticated requests
           started being blocked. Since then, Breadboard has been granted an unlimited R34 API key.
           As a result, we no longer need to store personal API keys from the user for R34. */
        if (lastUsedVersionCode in 305 .. 306) {
            val data = dataStore.data.first()
            val auths = data[PreferenceKeys.IMAGE_BOARD_AUTHS]
            if (auths != null) {
                val decodedAuths: Map<ImageSource, ImageBoardAuth> = Cbor.decodeFromByteArray(auths)
                if (decodedAuths[ImageSource.R34] != null) {
                    setAuth(ImageSource.R34, null, null)
                }
            }
        }

        /* Version 3.1.0 changes experiments to be a set rather than individual Pref items.
           Migrate the old "search pull to refresh" and "always animate scroll to top" settings
           to the new experiments set. */
        if (lastUsedVersionCode < 310) {
            val data = dataStore.data.first()
            val enabledExperiments = Prefs.DEFAULT.enabledExperiments.toMutableSet()
            val searchPtrKey = booleanPreferencesKey("search_pull_to_refresh")
            val alwaysAnimateScrollKey = booleanPreferencesKey("always_animate_scroll")
            val alwaysAnimateScroll = data[alwaysAnimateScrollKey] ?: false

            if (alwaysAnimateScroll) {
                enabledExperiments.add(Experiment.ALWAYS_ANIMATE_SCROLL)
            }

            updateSet(PreferenceKeys.ENABLED_EXPERIMENTS, enabledExperiments.map { it.name })
            dataStore.edit { prefs ->
                prefs.remove(searchPtrKey) // Now enabled by default
                prefs.remove(alwaysAnimateScrollKey)
            }
        }

        /* Version 3.1.1 removes the pref to show unfollowed tags in the frequents list.
           The option still exists at this time but is no longer persisted across sessions. */
        if (lastUsedVersionCode == 310) {
            val showIgnoredKey = booleanPreferencesKey("show_unfollowed_tags_in_frequents_list")
            dataStore.edit { prefs ->
                prefs.remove(showIgnoredKey)
            }
        }

        /* Version 3.1.4 (3.2.0) fixes HTML-encoded tags in favourites. */
        if (lastUsedVersionCode < 314) { // TODO: Change to 320 because 3.1.4 won't officially exist
            val data = dataStore.data.first()
            val favImagesByteArray = data[PreferenceKeys.FAVOURITE_IMAGES]
            if (favImagesByteArray != null) {
                val favImages: List<Image> = Cbor.decodeFromByteArray(favImagesByteArray)
                val migrated = favImages.map { image ->
                    val meta = image.metadata
                    if (meta != null) {
                        image.copy(
                            metadata = meta.copy(
                                groupedTags = meta.groupedTags.map { group ->
                                    group.copy(tags = group.tags.map { it.decodeHtml() })
                                },
                                artists = meta.artists.map { it.decodeHtml() }
                            )
                        )
                    } else image
                }
                updateFavouriteImages(migrated)
            }
        }

        /* Always clear the internal ignored list on updates. */
        if (BuildConfig.VERSION_CODE != lastUsedVersionCode) {
            dataStore.edit { prefs ->
                prefs[PreferenceKeys.INTERNAL_IGNORE_LIST] = emptySet()
                prefs[PreferenceKeys.INTERNAL_IGNORE_LIST_TIMESTAMP] = 0
            }
        }

        // Place any future migrations above this line by checking the last used version code.
        if (BuildConfig.VERSION_CODE >= lastUsedVersionCode)
            updateLastUsedVersionCode()
    }


    private suspend fun <T> updatePrefMain(key: Preferences.Key<T>, to: T) {
        dataStore.edit { preferences ->
            preferences[key] = to
        }
    }


    private suspend fun updateLastUsedVersionCode() {
        updatePrefMain(PreferenceKeys.LAST_USED_VERSION_CODE, BuildConfig.VERSION_CODE)
    }


    suspend fun updatePref(key: Preferences.Key<Boolean>, to: Boolean) {
        updatePrefMain(key, to)
    }


    suspend fun updatePref(key: Preferences.Key<String>, to: PrefEnum<*>) {
        updatePrefMain(key, (to as Enum<*>).name)
    }


    suspend fun updatePref(key: Preferences.Key<String>, to: String) {
        updatePrefMain(key, to)
    }


    suspend fun updatePref(key: Preferences.Key<Int>, to: Int) {
        updatePrefMain(key, to)
    }


    suspend fun updateEnumList(key: Preferences.Key<String>, to: List<PrefEnum<*>>) {
        updatePref(key, to.joinToString(",") { (it as Enum<*>).name })
    }


    /** You should probably use [addToSet] or [removeFromSet] instead.
     *  Only use this if using the alternatives would complicate things. */
    suspend fun updateSet(key: Preferences.Key<Set<String>>, to: Collection<String>) {
        updatePrefMain(key, to.toSet())
    }


    suspend fun addToSet(key: Preferences.Key<Set<String>>, item: String) {
        val set = dataStore.data.first()[key]?.toMutableSet() ?: mutableSetOf()
        set.add(item)
        updateSet(key, set)
    }


    suspend fun removeFromSet(key: Preferences.Key<Set<String>>, item: String) {
        val set = dataStore.data.first()[key]?.toMutableSet() ?: mutableSetOf()
        set.remove(item)
        updateSet(key, set)
    }


    suspend fun addToSet(key: Preferences.Key<Set<String>>, item: PrefEnum<*>) {
        addToSet(key, (item as Enum<*>).name)
    }


    suspend fun removeFromSet(key: Preferences.Key<Set<String>>, item: PrefEnum<*>) {
        removeFromSet(key, (item as Enum<*>).name)
    }


    suspend fun replaceImageRatings(newRatings: Set<ImageRating>) {
        updateSet(PreferenceKeys.RATINGS_FILTER, newRatings.map { it.name })
    }


    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> updateByteArray(key: Preferences.Key<ByteArray>, to: T) {
        updatePrefMain(key, Cbor.encodeToByteArray(to))
    }


    private suspend fun updateFavouriteImages(images: List<Image>) {
        updateByteArray(PreferenceKeys.FAVOURITE_IMAGES, images)
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


    private suspend fun updateSearchHistory(to: List<SearchHistoryEntry>) {
        updateByteArray(PreferenceKeys.SEARCH_HISTORY, to)
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


    suspend fun setAuth(source: ImageSource, username: String?, apiKey: String?) {
        val auths = getPreferences.first().imageBoardAuths.toMutableMap()
        if (username == null && apiKey == null) {
            auths.remove(source)
        } else {
            auths[source] = ImageBoardAuth(username!!, apiKey!!)
        }
        updateByteArray(PreferenceKeys.IMAGE_BOARD_AUTHS, auths)
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


    inline fun <reified T : Enum<T>> parseSerializedEnumList(serializedString: String): List<T> {
        if (serializedString.isEmpty()) {
            return emptyList()
        }

        return serializedString
            .split(",")
            .map { enumValues<T>().first { item -> item.name == it} }
    }


    @OptIn(ExperimentalSerializationApi::class)
    private fun mapUserPreferences(preferences: Preferences): Prefs {
        val dataSaver = preferences[PreferenceKeys.DATA_SAVER]?.let { DataSaver.valueOf(it) } ?: Prefs.DEFAULT.dataSaver
        val storageLocation = preferences[PreferenceKeys.STORAGE_LOCATION]?.toUri() ?: Prefs.DEFAULT.storageLocation

        val favouriteImagesRaw = preferences[PreferenceKeys.FAVOURITE_IMAGES]
        val favouriteImages: List<Image> = favouriteImagesRaw?.let { Cbor.decodeFromByteArray(it) } ?: Prefs.DEFAULT.favouriteImages

        val excludeAi = preferences[PreferenceKeys.EXCLUDE_AI] ?: Prefs.DEFAULT.excludeAi
        val imageSource = preferences[PreferenceKeys.IMAGE_SOURCE]?.let { ImageSource.valueOf(it) } ?: Prefs.DEFAULT.imageSource
        val favouritesFilter = (preferences[PreferenceKeys.FAVOURITES_FILTER]?.map { ImageSource.valueOf(it) } ?: Prefs.DEFAULT.favouritesFilter)
        val lastUsedVersionCode = preferences[PreferenceKeys.LAST_USED_VERSION_CODE] ?: Prefs.DEFAULT.lastUsedVersionCode
        val ratingsFilter = (preferences[PreferenceKeys.RATINGS_FILTER]?.map { ImageRating.valueOf(it) } ?: Prefs.DEFAULT.ratingsFilter)
        val favouritesRatingFilter = (preferences[PreferenceKeys.FAVOURITES_RATING_FILTER]?.map { ImageRating.valueOf(it) } ?: Prefs.DEFAULT.favouritesRatingsFilter)
        val filterRatingsLocally = (preferences[PreferenceKeys.FILTER_RATINGS_LOCALLY] ?: Prefs.DEFAULT.filterRatingsLocally)
        val useStaggeredGrid = (preferences[PreferenceKeys.USE_STAGGERED_GRID] ?: Prefs.DEFAULT.useStaggeredGrid)
        val saveSearchHistory = (preferences[PreferenceKeys.SAVE_SEARCH_HISTORY] ?: Prefs.DEFAULT.saveSearchHistory)

        val searchHistoryRaw = preferences[PreferenceKeys.SEARCH_HISTORY]
        val searchHistory: List<SearchHistoryEntry> = searchHistoryRaw?.let { Cbor.decodeFromByteArray(it) } ?: Prefs.DEFAULT.searchHistory

        val useFixedLinks = (preferences[PreferenceKeys.USE_FIXED_LINKS] ?: Prefs.DEFAULT.useFixedLinks)

        val imageBoardAuthsRaw = preferences[PreferenceKeys.IMAGE_BOARD_AUTHS]
        val imageBoardAuths: Map<ImageSource, ImageBoardAuth> = imageBoardAuthsRaw?.let { Cbor.decodeFromByteArray(it) } ?: Prefs.DEFAULT.imageBoardAuths

        val manuallyBlockedTags = preferences[PreferenceKeys.MANUALLY_BLOCKED_TAGS] ?: Prefs.DEFAULT.manuallyBlockedTags

        /* If an update adds a new ToolbarAction, add it to the users prefs.
           This seemed better than making a new migration every time there's a new action. */

        val imageViewerActions = preferences[PreferenceKeys.IMAGE_VIEWER_ACTION_ORDER]
            ?.let { parseSerializedEnumList<ToolbarAction>(it) }
            ?.let {
                it.toMutableList().apply {
                    for (action in ToolbarAction.entries) {
                        if (action !in this) {
                            add(action)
                        }
                    }
                }
            }
            ?: Prefs.DEFAULT.imageViewerActions

        val defaultStartDestination = preferences[PreferenceKeys.DEFAULT_START_DESTINATION]?.let { StartDestination.valueOf(it) } ?: Prefs.DEFAULT.defaultStartDestination
        val recommendAllRatings = preferences[PreferenceKeys.RECOMMEND_ALL_RATINGS] ?: Prefs.DEFAULT.recommendAllRatings
        val enabledExperiments = preferences[PreferenceKeys.ENABLED_EXPERIMENTS]?.mapNotNull {
            try {
                Experiment.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }?.toSet() ?: Prefs.DEFAULT.enabledExperiments
        val hasVerifiedAge = preferences[PreferenceKeys.HAS_VERIFIED_AGE] ?: Prefs.DEFAULT.getInternalAgeVerificationStatus()
        val flagSecureMode = preferences[PreferenceKeys.FLAG_SECURE_MODE]?.let { FlagSecureMode.valueOf(it) } ?: Prefs.DEFAULT.flagSecureMode
        val unfollowedTags = preferences[PreferenceKeys.UNFOLLOWED_TAGS] ?: Prefs.DEFAULT.unfollowedTags
        val recommendationsTagCount = preferences[PreferenceKeys.RECOMMENDATIONS_TAG_COUNT] ?: Prefs.DEFAULT.recommendationsTagCount
        val recommendationsPoolSize = preferences[PreferenceKeys.RECOMMENDATIONS_POOL_SIZE] ?: Prefs.DEFAULT.recommendationsPoolSize
        val recommendationsWeightedSelection = preferences[PreferenceKeys.RECOMMENDATIONS_WEIGHTED_SELECTION] ?: Prefs.DEFAULT.recommendationsWeightedSelection
        val internalIgnoreListTimestamp = preferences[PreferenceKeys.INTERNAL_IGNORE_LIST_TIMESTAMP] ?: Prefs.DEFAULT.internalIgnoreListTimestamp
        val internalIgnoreList = preferences[PreferenceKeys.INTERNAL_IGNORE_LIST] ?: Prefs.DEFAULT.internalIgnoreList
        val autoplayVideos = preferences[PreferenceKeys.AUTOPLAY_VIDEOS]?.let { AutoplayVideosMode.valueOf(it) } ?: Prefs.DEFAULT.autoplayVideos

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
            searchHistory,
            useFixedLinks,
            imageBoardAuths,
            manuallyBlockedTags,
            imageViewerActions,
            defaultStartDestination,
            recommendAllRatings,
            enabledExperiments,
            hasVerifiedAge,
            flagSecureMode,
            unfollowedTags,
            recommendationsTagCount,
            recommendationsPoolSize,
            recommendationsWeightedSelection,
            internalIgnoreListTimestamp,
            internalIgnoreList,
            autoplayVideos
        )
    }
}


// I don't know why Proguard is removing this.
@Keep
enum class ImageSource(override val label: String, val imageBoard: ImageBoard) : PrefEnum<ImageSource> {
    SAFEBOORU("Safebooru", Safebooru),
    DANBOORU("Danbooru", Danbooru),
    GELBOORU("Gelbooru", Gelbooru),
    YANDERE("Yande.re", Yandere),
    R34("Rule34", Rule34)
}
