package moe.apex.breadboard.saucenao

import moe.apex.breadboard.social.SocialSite
import moe.apex.breadboard.navigation.ImageView


/**
 * A group of [SauceNaoResult]s that (probably) represent the same artwork.
 *
 * The [primaryResult] is the result with the highest similarity in the group.
 * [relatedResults] are the remaining results, sorted by descending similarity.
 *
 * [mergedUrls] and [mergedImageBoards] aggregate the links from all results.
 */
data class SauceNaoResultGroup(
    val primaryResult: SauceNaoResult,
    val relatedResults: List<SauceNaoResult>,
    val mergedUrls: Map<SocialSite, List<String>>,
    val mergedImageBoards: List<ImageView>
)


/**
 * Groups a flat list of [SauceNaoResult]s by attempting to discern if they are the same work.
 *
 * Two results are considered the same work if they share:
 * - The same non-null [SauceNaoResultData.pixivId], or
 * - Any overlapping URL in their [SauceNaoResultData.extUrls]
 *
 * Results that don't match with any other result become groups with only a [SauceNaoResultGroup.primaryResult].
 * Groups are returned in the order of their primary result's position in the input list.
 */
fun groupResults(results: List<SauceNaoResult>): List<SauceNaoResultGroup> {
    /* This whole thing is awkward.
       Unfortunately it's already late and I don't know what I'm doing any more.
       Future me can revisit at some point, but for now we're keeping it. */

    if (results.isEmpty()) return emptyList()

    val parent = IntArray(results.size) { it }

    fun find(x: Int): Int {
        var root = x
        while (parent[root] != root) root = parent[root]
        var curr = x
        while (curr != root) {
            val next = parent[curr]
            parent[curr] = root
            curr = next
        }
        return root
    }

    fun union(a: Int, b: Int) {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA != rootB) parent[rootA] = rootB
    }

    // Dumb
    val pixivIdToIndex = mutableMapOf<Int, Int>()
    val urlToIndex = mutableMapOf<String, Int>()

    for (i in results.indices) {
        val data = results[i].data

        // Start grouping using the pixiv id
        data.pixivId?.let { pxId ->
            val existing = pixivIdToIndex[pxId]
            if (existing != null) {
                union(i, existing)
            } else {
                pixivIdToIndex[pxId] = i
            }
        }

        // Group by any shared extUrls (which can also contain the source url)
        for (url in data.extUrls) {
            val existing = urlToIndex[url]
            if (existing != null) {
                union(i, existing)
            } else {
                urlToIndex[url] = i
            }
        }
    }

    // Collect groups, root -> list of indices
    val groups = mutableMapOf<Int, MutableList<Int>>()
    for (i in results.indices) {
        groups.getOrPut(find(i)) { mutableListOf() }.add(i)
    }

    // Build SauceNaoResultGroups, ordered by the earliest index in each group
    return groups.values
        .sortedBy { it.min() }
        .map { indices ->
            val sorted = indices.sortedByDescending { results[it].header.similarity }
            val primary = results[sorted.first()]
            val related = sorted.drop(1).map { results[it] }

            val allResults = sorted.map { results[it] }

            // Merge social URLs from all results in the group
            val mergedUrls = mutableMapOf<SocialSite, MutableList<String>>()
            for (result in allResults) {
                for ((site, urls) in result.data.mapUrls()) {
                    val existing = mergedUrls.getOrPut(site) { mutableListOf() }
                    for (url in urls) {
                        if (url !in existing) {
                            existing.add(url)
                        }
                    }
                }
            }

            // Also merge the provided imageboard sources
            val mergedImageBoards = mutableListOf<ImageView>()
            for (result in allResults) {
                for (imageView in result.data.parseImageBoards()) {
                    if (mergedImageBoards.none { it.source == imageView.source && it.id == imageView.id }) {
                        mergedImageBoards.add(imageView)
                    }
                }
            }

            SauceNaoResultGroup(
                primaryResult = primary,
                relatedResults = related,
                mergedUrls = mergedUrls,
                mergedImageBoards = mergedImageBoards
            )
        }
}
