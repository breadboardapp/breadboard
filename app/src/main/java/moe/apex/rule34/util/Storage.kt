package moe.apex.rule34.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.image.Image
import moe.apex.rule34.prefs
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.IOException


class MustSetLocation(message: String): Exception(message)
class UnsupportedFileType(message: String): Exception(message)


@Composable
fun SaveDirectorySelection(requester: MutableState<Boolean>) {
    val context = LocalContext.current
    val prefs = context.prefs
    val scope = rememberCoroutineScope()

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedUri = result.data?.data
                if (selectedUri != null) {
                    val tree = DocumentsContract.buildDocumentUriUsingTree(
                        selectedUri, DocumentsContract.getTreeDocumentId(selectedUri)
                    )
                    scope.launch { prefs.updateStorageLocation(tree) }
                }
            } else {
                Toast.makeText(context, "Nothing selected.", Toast.LENGTH_SHORT).show()
            }
            requester.value = false
        }

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    SideEffect {
        requestPermissionLauncher.launch(intent)
    }
}

private val client = OkHttpClient()

suspend fun downloadImage(context: Context, image: Image, location: Uri): Result<Boolean> {
    if (location == Uri.EMPTY) {
        return Result.failure(MustSetLocation("Set save location and try again."))
    }
    val fileName = image.fileName
    val url = image.highestQualityFormatUrl
    val mimeType = when (image.fileFormat) {
        "jpeg", "jpg" -> "image/jpeg"
        "png"         -> "image/png"
        "gif"         -> "image/gif"
        else -> return Result.failure(UnsupportedFileType("Unsupported file format."))
    }

    val outputFolder = DocumentFile.fromTreeUri(context, location)
    val outputFile = outputFolder!!.createFile(mimeType, fileName)
        ?: return Result.failure(MustSetLocation("Set save location and try again."))

    val outputUri = outputFile.uri
    val request = Request.Builder().url(url).build()

    return context.contentResolver.openOutputStream(outputUri).use { file ->
        withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(response.code.toString()))
                }

                response.body?.source()?.use { source ->
                    file!!.sink().buffer().use { sink ->
                        sink.writeAll(source)
                    }
                }

                Result.success(true)
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }
    }
}
