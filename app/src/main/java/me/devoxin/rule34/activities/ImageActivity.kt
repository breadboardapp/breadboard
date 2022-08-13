package me.devoxin.rule34.activities

import android.Manifest
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.jsibbold.zoomage.ZoomageView
import com.squareup.picasso.Picasso
import me.devoxin.rule34.R
import java.io.File

class ImageActivity: AppCompatActivity() {
    private val circularProgressDrawable: CircularProgressDrawable
        get() = CircularProgressDrawable(this).apply {
            strokeWidth = 20f
            centerRadius = 100f
            setColorSchemeColors(R.color.colorAccent)
            start()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.full_image)

        val view = findViewById<ZoomageView>(R.id.imageView)
        val imageUri = intent.getStringExtra("img")
            ?: throw IllegalStateException("Img key not set")

        findViewById<Button>(R.id.hd_button).isEnabled = intent.getBooleanExtra("hdAvailable", false)

        Picasso.get()
            .load(imageUri)
            //.fit()
            //.centerInside()
            .placeholder(circularProgressDrawable)
            .into(view)
    }

    fun onHdClick(v: View) {
        val hdUrl = intent.getStringExtra("hd")
        val view = findViewById<ZoomageView>(R.id.imageView)
        v.isEnabled = false

        Picasso.get()
            .load(hdUrl)
            //.fit()
            //.centerInside()
            .placeholder(circularProgressDrawable)
            .into(view)
    }

    fun onSaveClick(v: View) {
        val image = findViewById<ZoomageView>(R.id.imageView)
        val bitmap = (image.drawable as BitmapDrawable).bitmap
        val fileName = intent.getStringExtra("fileName")!!

        val compressFormat = when (intent.getStringExtra("fileFormat")!!) {
            "jpeg", "jpg", "gif" -> Bitmap.CompressFormat.JPEG
            "png" -> Bitmap.CompressFormat.PNG
            else -> throw UnsupportedOperationException("Unsupported file format")
        }
        val fileFormat = compressFormat.name.lowercase()

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
//        val permissionIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
//        startActivity(permissionIntent)

        val appDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "rule34")

        if (appDirectory.exists() || appDirectory.mkdirs()) {
            val output = File(appDirectory, "${fileName}_2.$fileFormat").apply {
                if (exists()) delete()
            }

            MediaScannerConnection.scanFile(this, arrayOf(output.absolutePath), arrayOf("image/$fileFormat")) { path, uri ->
                if (output.createNewFile()) {
                    output.outputStream().use {
                        bitmap.compress(compressFormat, 100, it)
                    }
                }
            }
            v.isEnabled = false
        }
    }
}
