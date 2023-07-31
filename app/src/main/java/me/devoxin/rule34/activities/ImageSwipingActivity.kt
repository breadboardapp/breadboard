package me.devoxin.rule34.activities

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import com.jsibbold.zoomage.ZoomageView
import me.devoxin.rule34.Image
import me.devoxin.rule34.R
import java.io.File
import java.lang.Exception
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

//        Picasso.get()
//            .load(images[currentPosition].highestQualityFormatUrl)
//            .placeholder(circularProgressDrawable)
//            .into(pager.findViewWithTag<ZoomageView>("View$currentPosition"))

        Glide.with(this)
            .load(images[currentPosition].highestQualityFormatUrl)
            .placeholder(circularProgressDrawable)
            .into(pager.findViewWithTag<ZoomageView>("View$currentPosition"))
    }

    inner class ViewPagerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val zv = view.findViewById<ZoomageView>(R.id.zoom_view)

        fun setData(imageUrl: String) {
//            Picasso.get()
//                .load(imageUrl)
//                .placeholder(circularProgressDrawable)
//                .into(zv)

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

            // TODO: Fix saving
            findViewById<Button>(R.id.save_button).setOnClickListener {
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

                    MediaScannerConnection.scanFile(this@ImageSwipingActivity, arrayOf(output.absolutePath), arrayOf("image/$fileFormat")) { path, uri ->
                        if (output.createNewFile()) {
                            it.isEnabled = false

//                            Picasso.get()
//                                .load(imageUrl)
//                                .into(object : Target {
//                                    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
//                                        output.outputStream().use { stream ->
//                                            bitmap.compress(compressFormat, 100, stream)
//                                        }
//                                    }
//
//                                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
//                                        it.isEnabled = true
//                                        Toast.makeText(this@ImageSwipingActivity, "uh oh fucky wucky!", Toast.LENGTH_SHORT).show()
//                                    }
//
//                                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) = Unit
//                                })
                        }
                    }
                }
            }
        }
    }
}
