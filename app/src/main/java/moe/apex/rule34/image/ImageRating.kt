package moe.apex.rule34.image

import android.util.Log


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
        private val mapping = mapOf(
            SAFE to FILTER_SAFE,
            SENSITIVE to FILTER_SENSITIVE,
            QUESTIONABLE to FILTER_QUESTIONABLE,
            EXPLICIT to FILTER_EXPLICIT
        )

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

        fun buildSearchStringFor(vararg ratings: ImageRating): String {
            val currentFilter = mutableListOf(FILTER_SAFE, FILTER_SENSITIVE, FILTER_QUESTIONABLE, FILTER_EXPLICIT)
            for (rating in ratings) {
                if (rating in mapping) {
                    currentFilter.remove(mapping[rating])
                }
            }
            return currentFilter.joinToString("+")
        }
    }
}