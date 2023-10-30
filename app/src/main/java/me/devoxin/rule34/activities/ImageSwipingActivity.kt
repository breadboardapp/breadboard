package me.devoxin.rule34.activities

import android.Manifest
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.jsibbold.zoomage.ZoomageView
import me.devoxin.rule34.Image
import me.devoxin.rule34.R
import java.io.File
import java.util.ArrayList

class ImageSwipingActivity : AppCompatActivity() {
    private val circularProgressDrawable: CircularProgressDrawable
        get() = CircularProgressDrawable(this).apply {
            strokeWidth = 10f
            centerRadius = 50f
            setColorSchemeColors(R.color.colorAccent)
            start()
        }

    private lateinit var images: ArrayList<Image>

    private var currentView: ViewPagerViewHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.full_image_swiping)

        images = intent.getParcelableArrayListExtra("images")!!

        val position = intent.getIntExtra("position", 0)
        val view = findViewById<ViewPager2>(R.id.viewpager)
        view.adapter = ViewPagerAdapter()
        view.setCurrentItem(position, false)
        findViewById<Button>(R.id.hd_button).isEnabled = images[position].hdAvailable

        view.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                findViewById<Button>(R.id.hd_button).isEnabled = images[position].hdAvailable
            }
        })

//        Picasso.get()
//            .load(imageUri)
//            //.fit()
//            //.centerInside()
//            .placeholder(circularProgressDrawable)
//            .into(view)
    }

    fun onHdClick(v: View) {
        val pager = findViewById<ViewPager2>(R.id.viewpager)
        val currentPosition = pager.currentItem

        v.isEnabled = false

        Glide.with(this)
            .load(images[currentPosition].highestQualityFormatUrl)
            .placeholder(circularProgressDrawable)
            .into(pager.findViewWithTag<ZoomageView>("View$currentPosition"))
    }

    fun onSaveClick(v: View) {
        val pager = findViewById<ViewPager2>(R.id.viewpager)
        val currentPosition = pager.currentItem
        val image = images[currentPosition]

        val imageUrl = image.highestQualityFormatUrl
        val fileName = image.fileName

        val compressFormat = when (image.fileFormat) {
            "jpeg", "jpg", "gif" -> Bitmap.CompressFormat.JPEG
            "png" -> Bitmap.CompressFormat.PNG
            else -> throw UnsupportedOperationException("Unsupported file format")
        }

        val fileFormat = compressFormat.name.lowercase()

        ActivityCompat.requestPermissions(this@ImageSwipingActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        val appDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "rule34")

        if (appDirectory.exists() || appDirectory.mkdirs()) {
            val output = File(appDirectory, "${fileName}_2.$fileFormat").apply {
                if (exists()) delete()
            }

            MediaScannerConnection.scanFile(this, arrayOf(output.absolutePath), arrayOf("image/$fileFormat")) { _, _ ->
                @Suppress("BlockingMethodInNonBlockingContext")
                if (!output.createNewFile()) {
                    return@scanFile Toast.makeText(this, "Failed to create file!", Toast.LENGTH_SHORT).show()
                }

                v.isEnabled = false

                Glide.with(this)
                    .asBitmap()
                    .load(imageUrl)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            output.outputStream().use { stream ->
                                resource.compress(compressFormat, 100, stream)
                            }

                            Toast.makeText(this@ImageSwipingActivity, "Image saved", Toast.LENGTH_SHORT).show()
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            v.isEnabled = true
                            Toast.makeText(this@ImageSwipingActivity, "Failed to download image", Toast.LENGTH_SHORT).show()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) = Unit
                    })
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
        override fun getItemCount() = images.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.single_zoom_view, parent, false)
            return ViewPagerViewHolder(view).also { currentView = it }
        }

        override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
            val image = images[position]
            holder.setData(image.defaultUrl)
            holder.zv.tag = "View$position"
        }
    }
}
