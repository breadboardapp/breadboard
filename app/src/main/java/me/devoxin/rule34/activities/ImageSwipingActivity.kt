package me.devoxin.rule34.activities

import android.Manifest
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewpager2.widget.ViewPager2
import com.bugsnag.android.Bugsnag
import com.bumptech.glide.Glide
import com.jsibbold.zoomage.ZoomageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.devoxin.rule34.ImageSource
import me.devoxin.rule34.R
import me.devoxin.rule34.util.HttpUtil
import me.devoxin.rule34.util.Scopes
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ImageSwipingActivity : AuthenticatableActivity() {
    private val circularProgressDrawable: CircularProgressDrawable
        get() = CircularProgressDrawable(this).apply {
            strokeWidth = 10f
            centerRadius = 50f
            setColorSchemeColors(R.color.colorAccent)
            start()
        }

    private var currentView: ViewPagerViewHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.full_image_swiping)

        val position = intent.getIntExtra("position", 0)
        val view = findViewById<ViewPager2>(R.id.viewpager).apply {
            adapter = ViewPagerAdapter()
            setCurrentItem(position, false)
        }

        findViewById<Button>(R.id.hd_button).isEnabled = ImageSource.images[position].hdAvailable

        view.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                findViewById<Button>(R.id.hd_button).isEnabled = ImageSource.images[position].hdAvailable
            }
        })
    }

    fun onHdClick(v: View) {
        val pager = findViewById<ViewPager2>(R.id.viewpager)
        val currentPosition = pager.currentItem

        v.isEnabled = false

        Glide.with(this)
            .load(ImageSource.images[currentPosition].highestQualityFormatUrl)
            .placeholder(circularProgressDrawable)
            .into(pager.findViewWithTag<ZoomageView>("View$currentPosition"))
    }

    fun onSaveClick(v: View) {
        Scopes.MAIN.launch {
            val pager = findViewById<ViewPager2>(R.id.viewpager)
            val currentPosition = pager.currentItem
            val image = ImageSource.images[currentPosition]

            val imageUrl = image.highestQualityFormatUrl
            val fileName = image.fileName

            val (fileFormat, contentType) = when (image.fileFormat) {
                "jpeg", "jpg" -> "jpg" to "image/jpeg"
                "gif"         -> "gif" to "image/gif"
                "png"         -> "png" to "image/png"
                else -> throw UnsupportedOperationException("Unsupported file format")
            }

            ActivityCompat.requestPermissions(this@ImageSwipingActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

            val appDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "rule34")

            if (appDirectory.exists() || appDirectory.mkdirs()) {
                val output = File(appDirectory, "${fileName}_2.$fileFormat").apply {
                    if (exists()) delete()
                }

                v.isEnabled = false

                val fileCreated = withContext(Dispatchers.IO) { output.createNewFile() }

                if (!fileCreated) {
                    return@launch Toast.makeText(this@ImageSwipingActivity, "Failed to create file!", Toast.LENGTH_SHORT).show()
                }

                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        HttpUtil.get(imageUrl).byteStream().use { src ->
                            output.outputStream().use { file ->
                                src.copyTo(file)
                            }
                        }
                    }.onFailure {
                        v.isEnabled = true
                        Bugsnag.notify(it)
                    }
                }

                if (result.isSuccess) {
                    Toast.makeText(this@ImageSwipingActivity, "Image downloaded", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ImageSwipingActivity, "Failed to download image: ${result.exceptionOrNull()!!.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class ViewPagerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val zv = view.findViewById<ZoomageView>(R.id.zoom_view)

        fun setData(imageUrl: String) {
            Glide.with(this@ImageSwipingActivity)
                .load(imageUrl)
                .placeholder(circularProgressDrawable)
                .into(zv)
        }
    }

    inner class ViewPagerAdapter : RecyclerView.Adapter<ImageSwipingActivity.ViewPagerViewHolder>() {
        override fun getItemCount() = ImageSource.itemCount

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.single_zoom_view, parent, false)
            return ViewPagerViewHolder(view).also { currentView = it }
        }

        override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
            val image = ImageSource.images[position]
            holder.setData(image.defaultUrl)
            holder.zv.tag = "View$position"
        }
    }
}
