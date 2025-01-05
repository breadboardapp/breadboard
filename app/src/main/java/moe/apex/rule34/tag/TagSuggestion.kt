package moe.apex.rule34.tag

import java.util.Objects

class TagSuggestion(
    val label: String,
    val value: String,
    val category: String?,
    val isExcluded: Boolean
) {
    val formattedLabel = if (isExcluded) "-$value" else value

    override fun equals(other: Any?): Boolean {
        return other is TagSuggestion && other.label == this.label
    }

    override fun hashCode(): Int {
        return Objects.hashCode(value)
    }
}
