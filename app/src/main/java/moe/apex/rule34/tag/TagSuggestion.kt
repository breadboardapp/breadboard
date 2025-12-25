package moe.apex.rule34.tag

import kotlinx.serialization.Serializable
import java.util.Objects

@Serializable
class TagSuggestion(
    val label: String,
    val value: String,
    val category: String?,
    val isExcluded: Boolean
) {
    val formattedLabel = if (isExcluded) "-$value" else value

    override fun equals(other: Any?): Boolean {
        return other is TagSuggestion && other.formattedLabel == this.formattedLabel
    }

    override fun hashCode(): Int {
        return Objects.hashCode(value)
    }
}
