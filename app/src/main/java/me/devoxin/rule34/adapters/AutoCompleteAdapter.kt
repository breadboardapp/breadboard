package me.devoxin.rule34.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import me.devoxin.rule34.R
import me.devoxin.rule34.RequestUtil
import org.json.JSONArray
import java.util.*
import kotlin.collections.ArrayList

class AutoCompleteAdapter(private val context: Context) : BaseAdapter(), Filterable {
    private var elements = ArrayList<TagSuggestion>()

    override fun getCount(): Int = elements.size

    override fun getItem(position: Int) = elements[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val convView = convertView ?: LayoutInflater.from(context).inflate(R.layout.two_line_list_item, parent, false)
        val item = elements[position]
        convView.findViewById<TextView>(R.id.titleText).text = item.label
        convView.findViewById<TextView>(R.id.categoryText).text = item.type
        return convView
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()

                if (constraint != null) {
                    val tagExclusion = constraint.startsWith("-")
                    val query = constraint.replace("^-".toRegex(), "")
                    val body = RequestUtil.get("https://rule34.xxx/public/autocomplete.php?q=$query").get()
                    val results = JSONArray(body)
                    val resultCount = results.length()
                    val suggestions = ArrayList<TagSuggestion>()

                    for (i in 0 until resultCount) {
                        val suggestion = results.getJSONObject(i)
                        val label = suggestion.getString("label")
                        val value = suggestion.getString("value")
                        val type = suggestion.getString("type")
                        suggestions.add(TagSuggestion(label, value, type, tagExclusion))
                    }

                    filterResults.apply {
                        values = suggestions
                        count = suggestions.size
                    }
                }

                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                results?.takeIf { it.count > 0 }?.let {
                    @Suppress("UNCHECKED_CAST")
                    elements = it.values as ArrayList<TagSuggestion>
                    notifyDataSetChanged()
                } ?: notifyDataSetInvalidated()
            }
        }
    }

    class TagSuggestion(val label: String, val value: String, val type: String, val exclude: Boolean = false) {
        val formattedTag = if (exclude) "-$value" else value

        override fun equals(other: Any?): Boolean {
            return other is TagSuggestion && other.value == this.value
        }

        override fun hashCode(): Int {
            return Objects.hashCode(value)
        }
    }
}
