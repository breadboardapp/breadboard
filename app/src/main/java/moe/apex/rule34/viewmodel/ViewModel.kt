package moe.apex.rule34.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import moe.apex.rule34.tag.TagSuggestion
import moe.apex.rule34.util.RecommendationsProvider


class BreadboardViewModel : ViewModel() {
    val tagSuggestions = mutableStateListOf<TagSuggestion>()
    var recommendationsProvider: RecommendationsProvider? = null
}


fun SnapshotStateList<TagSuggestion>.getIndexByName(name: String): Int? {
    this.forEachIndexed { index, tag ->
        if (tag.value == name) return index
    }
    return null
}
