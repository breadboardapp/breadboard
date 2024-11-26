package moe.apex.rule34.image


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

        fun buildSearchStringFor(ratings: List<ImageRating>): String {
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
