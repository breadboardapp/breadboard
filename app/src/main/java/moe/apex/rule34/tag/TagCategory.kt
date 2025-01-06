package moe.apex.rule34.tag

import kotlinx.serialization.Serializable

enum class TagCategory(val label: String, val pluralisedLabel: String) {
    ARTIST("Artist", "Artists"),
    CHARACTER("Character", "Characters"),
    COPYRIGHT("Copyright", "Copyrights"),
    GENERAL("Tag", "Tags"),
    META("Meta", "Meta");

    fun group(tags: List<String>): TagGroup {
        return TagGroup(this, tags.filter { it.isNotEmpty() })
    }
}

@Serializable
data class TagGroup(val category: TagCategory, val tags: List<String>)