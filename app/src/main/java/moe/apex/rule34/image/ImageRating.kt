package moe.apex.rule34.image

import android.util.Log
import moe.apex.rule34.preferences.ImageSource


private const val FILTER_SAFE         = "-rating:safe+-rating:general+-rating:g"
private const val FILTER_SENSITIVE    = "-rating:sensitive+-rating:s"
private const val FILTER_QUESTIONABLE = "-rating:questionable+-rating:q"
private const val FILTER_EXPLICIT     = "-rating:explicit+-rating:e"


enum class ImageRating(val label: String) {
    SAFE("Safe"),
    SENSITIVE("Sensitive"),
    QUESTIONABLE("Questionable"),
    EXPLICIT("Explicit"),
    UNKNOWN("Unknown");

    /* Why are their naming schemes so inconsistent lol
       The Gelbooru help page doesn't mention "sensitive" at all but posts seem to regularly use it.
       Likewise, Safebooru seems to use both "general" and "safe" for some reason. */
    companion object {
        fun fromString(label: String): ImageRating {
            return when (label.lowercase()) {
                "safe", "general", "g" -> SAFE
                "sensitive", "s"       -> SENSITIVE
                "questionable", "q"    -> QUESTIONABLE
                "explicit", "e"        -> EXPLICIT
                else -> {
                    Log.w("ImageRating", "Unknown rating label: $label")
                    UNKNOWN
                }
            }
        }
    }

    /* These image boards are so inconsistent that it's actually more reliable to search by
       unwanted tags than by wanted tags. I hate it too.
       Danbooru and Gelbooru seem to be the only ones that use "sensitive" but there's also no real
       harm in adding it to the filter for other sites. */
    fun toSearchFilters(source: ImageSource): List<String> {
        return when (this) {
            SAFE         -> listOf(FILTER_SENSITIVE, FILTER_QUESTIONABLE, FILTER_EXPLICIT)
            SENSITIVE    -> listOf(FILTER_SAFE, FILTER_QUESTIONABLE, FILTER_EXPLICIT)
            QUESTIONABLE -> listOf(FILTER_SAFE, FILTER_SENSITIVE, FILTER_EXPLICIT)
            EXPLICIT     -> listOf(FILTER_SAFE, FILTER_SENSITIVE, FILTER_QUESTIONABLE)
            UNKNOWN      -> emptyList()
        }
    }
}