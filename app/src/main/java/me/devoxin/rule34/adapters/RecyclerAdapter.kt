package me.devoxin.rule34.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bugsnag.android.Bugsnag
import com.bumptech.glide.Glide
import me.devoxin.rule34.Image
import me.devoxin.rule34.ImageSource
import me.devoxin.rule34.R
import me.devoxin.rule34.activities.ImageSwipingActivity
import java.io.IOException
import java.net.UnknownHostException

class RecyclerAdapter(
    private val context: Context,
    vararg tags: String,
    private val toastCallback: (String) -> Unit,
    private val finishCallback: (String) -> Unit
): RecyclerView.Adapter<RecyclerAdapter.ResultViewHolder>() {
    private var items = mutableListOf<Image>()
    private val sourceLoader = ImageSource(*tags)

    private var isLoading = false

    init {
        loadMore()
    }

    fun loadMore() {
        // api has hard limit of 1000, so cut requests at page = 10?
        Log.d("imageLoader", isLoading.toString())
        if (!isLoading) {
            isLoading = true

            try {
                Log.d("imageLoader", "Loading next page")
                val pageItems = sourceLoader.nextPage()
                Log.d("imageLoader", "${pageItems.size} fetched items, ${items.size} stored items")

                // Consider adding an `isAtEnd` boolean to prevent repeated load requests.
                if (items.isEmpty() && pageItems.isEmpty()) {
                    return finishCallback("Query yielded no results")
                }

                val indexBegin = items.size - 1
                items.addAll(pageItems)
                notifyItemRangeChanged(indexBegin, pageItems.size)
            } catch (e: Exception) {
                val cause = e.let { it.cause ?: it }
                e.printStackTrace()

                if (cause is IOException || cause is UnknownHostException) {
                    return when {
                        items.isEmpty() -> finishCallback("A network error occurred whilst fetching images.")
                        else -> toastCallback("A network error occurred whilst fetching images.")
                    }
                } else {
                    Bugsnag.notify(e)
                }
            } finally {
                isLoading = false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.image_item, parent, false) // inflate (..., null)
        val image = view.findViewById<ImageView>(R.id.image_item)

        return ResultViewHolder(image)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val image = items[position]

//        Picasso.get()
//            .load(image.previewUrl)
//            .resize(300, 300)
//            .centerCrop()
//            .into(holder.view)

        Glide.with(context)
            .load(image.previewUrl)
            .override(300, 300)
            .centerCrop()
            .into(holder.view)


        holder.view.setOnClickListener { onItemClick(holder, position) }
    }

    override fun getItemCount(): Int = items.size

    private fun onItemClick(view: ResultViewHolder, position: Int) {
        val i = items[position]
        val int = Intent(context, ImageSwipingActivity::class.java)

        int.putExtra("position", items.indexOf(i))
        int.putParcelableArrayListExtra("images", ArrayList(items))

        context.startActivity(int)
    }


    override fun getItemViewType(position: Int): Int {
        return position
    }

    class ResultViewHolder(val view: ImageView): RecyclerView.ViewHolder(view)
}
