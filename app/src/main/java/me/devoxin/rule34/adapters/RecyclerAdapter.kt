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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.devoxin.rule34.ImageSource
import me.devoxin.rule34.R
import me.devoxin.rule34.activities.ImageSwipingActivity
import java.io.IOException

class RecyclerAdapter(
    private val context: Context,
    vararg tags: String,
    private val activityCallback: (Intent) -> Unit,
    private val toastCallback: (String) -> Unit,
    private val finishCallback: (String) -> Unit
): RecyclerView.Adapter<RecyclerAdapter.ResultViewHolder>() {
    private var isLoading = false
    private var noMoreResults = false

    init {
        ImageSource.withTags(*tags)
        loadMore()
    }

    fun loadMore() {
        MAIN_SCOPE.launch { loadMore0() }
    }

    private suspend fun loadMore0() {
        if (isLoading || noMoreResults) {
            return
        }

        // api has hard limit of 1000, so cut requests at page = 10?
        isLoading = true

        try {
            Log.d("RecyclerAdapter", "Loading next page")
            val currentImageCount = ImageSource.itemCount
            val pageImageCount = ImageSource.nextPage()
            val newImageCount = ImageSource.itemCount
            Log.d("RecyclerAdapter", "$pageImageCount fetched items, $newImageCount stored items")

            if (pageImageCount == 0) {
                if (newImageCount == 0) {
                    return finishCallback("Query yielded no results")
                }

                noMoreResults = true
            } else {
                Log.d("RecyclerAdapter", "Notifying item range inserted with values ($currentImageCount, $newImageCount)")
                notifyItemRangeInserted(currentImageCount, pageImageCount)
            }
        } catch (e: Exception) {
            val cause = e.let { it.cause ?: it }
            e.printStackTrace()

            if (cause is IOException) { // || cause is UnknownHostException
                return when (ImageSource.itemCount) {
                    0 -> finishCallback("A network error occurred whilst fetching images.")
                    else -> toastCallback("A network error occurred whilst fetching images.")
                }
            } else {
                Bugsnag.notify(e)
            }
        } finally {
            isLoading = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.image_item, parent, false)
        val image = view.findViewById<ImageView>(R.id.image_item)

        return ResultViewHolder(image)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val image = ImageSource.images[position]

        Glide.with(context)
            .load(image.previewUrl)
            .override(300, 300)
            .centerCrop()
            .into(holder.view)

        holder.view.setOnClickListener { onItemClick(holder, position) }
    }

    override fun getItemCount() = ImageSource.itemCount

    private fun onItemClick(view: ResultViewHolder, position: Int) {
        val int = Intent(context, ImageSwipingActivity::class.java)
        int.putExtra("position", position)
        activityCallback(int)
    }


    override fun getItemViewType(position: Int): Int {
        return position
    }

    class ResultViewHolder(val view: ImageView): RecyclerView.ViewHolder(view)

    companion object {
        private val MAIN_SCOPE = CoroutineScope(Dispatchers.Main)
    }
}
