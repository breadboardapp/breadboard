package moe.apex.rule34.image

import moe.apex.rule34.preferences.PrefEnum


private val FILTER_SAFE         = listOf("-rating:safe", "-rating:general", "-rating:g")
private val FILTER_SENSITIVE    = listOf("-rating:sensitive", "-rating:s")
private val FILTER_QUESTIONABLE = listOf("-rating:questionable", "-rating:q")
private val FILTER_EXPLICIT     = listOf("-rating:explicit", "-rating:e")


enum class ImageRating(override val label: String) : PrefEnum<ImageRating> {
    SAFE("Safe"),
    SENSITIVE("Sensitive"),
    QUESTIONABLE("Questionable"),
    EXPLICIT("Explicit"),
    UNKNOWN("Unknown");

    /* Why are their naming schemes so inconsistent lol
       The Gelbooru help page doesn't mention "sensitive" at all but posts seem to regularly use it.
       Likewise, Safebooru seems to use both "general" and "safe" for some reason.

       Yande.re differs from other letter-based sources because "s" is safe rather than sensitive
       and sensitive does not exist.
       However, negative filtering by more than one rating doesn't work on Yande.re, so we're going
       to enforce the local rating option. */
    companion object {
        private val mapping = mapOf(
            SAFE to FILTER_SAFE,
            SENSITIVE to FILTER_SENSITIVE,
            QUESTIONABLE to FILTER_QUESTIONABLE,
            EXPLICIT to FILTER_EXPLICIT
        )


        fun buildQueryListFor(vararg ratings: ImageRating): List<List<String>> {
            val currentFilter = mutableListOf(FILTER_SAFE, FILTER_SENSITIVE, FILTER_QUESTIONABLE, FILTER_EXPLICIT)
            for (rating in ratings) {
                if (rating in mapping) {
                    currentFilter.remove(mapping[rating])
                }
            }
            return currentFilter
        }

        fun buildSearchStringFor(vararg ratings: ImageRating): String {
            val currentFilter = buildQueryListFor(*ratings)
            return currentFilter.joinToString("+") { it.joinToString("+") }
        }

        fun buildSearchStringFor(ratings: Collection<ImageRating>): String {
            return buildSearchStringFor(*ratings.toTypedArray())
        }
    }
}
