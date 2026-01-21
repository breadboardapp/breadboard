package moe.apex.rule34.util

import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.image.Image
import moe.apex.rule34.preferences.PreferenceKeys
import moe.apex.rule34.prefs
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.OutputStream


class MustSetLocation(message: String): Exception(message)
class UnsupportedFileType(message: String): Exception(message)


const val MIME_TYPE = "application/*"


enum class PromptType {
    DIRECTORY_PERMISSION,
    READ_FILE,
    CREATE_FILE
}


fun saveUriToPref(context: Context, scope: CoroutineScope, uri: Uri) {
    val tree = DocumentsContract.buildDocumentUriUsingTree(
        uri, DocumentsContract.getTreeDocumentId(uri)
    )
    scope.launch(Dispatchers.IO) {
        context.prefs.updatePref(PreferenceKeys.STORAGE_LOCATION, tree.toString())
    }
}


@Composable
fun StorageLocationSelection(
    promptType: PromptType,
    onFailure: () -> Unit = { },
    onSuccess: (Uri) -> Unit
) {
    val context = LocalContext.current

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedUri = result.data?.data
                if (selectedUri != null) {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(selectedUri, flags)
                    onSuccess(selectedUri)
                }
            } else {
                showToast(context, "Nothing selected.")
                onFailure()
            }
        }

    val intent = when (promptType) {
        PromptType.DIRECTORY_PERMISSION -> Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        PromptType.READ_FILE -> Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = MIME_TYPE
        }
        PromptType.CREATE_FILE -> Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, "breadboard_export.bread")
            type = MIME_TYPE
        }
    }

    SideEffect {
        requestPermissionLauncher.launch(intent)
    }
}

private val client = OkHttpClient()


private suspend fun downloadToStream(image: Image, outputStream: OutputStream): Result<Unit> {
    val url = image.highestQualityFormatUrl
    val request = Request.Builder().url(url).build()

    return withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(response.code.toString()))
                }

                response.body.byteStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }

                Result.success(Unit)
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}


suspend fun downloadImage(context: Context, image: Image, location: Uri): Result<Boolean> {
    if (location == Uri.EMPTY) {
        return Result.failure(MustSetLocation("Set save location and try again."))
    }

    val mimeType = getMimeType(image.fileFormat)
        ?: return Result.failure(UnsupportedFileType("Unsupported file format."))

    val fileName = image.fileName
    val outputFolder = DocumentFile.fromTreeUri(context, location)
    
    return withContext(Dispatchers.IO) {
        val outputFile = outputFolder!!.createFile(mimeType, fileName)
            ?: return@withContext Result.failure(MustSetLocation("Set save location and try again."))

        try {
            context.contentResolver.openOutputStream(outputFile.uri).use { outputStream ->
                if (outputStream == null) return@withContext Result.failure(IOException("Could not open output stream"))
                downloadToStream(image, outputStream).getOrThrow()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


suspend fun downloadImageToClipboard(context: Context, clipboard: Clipboard, image: Image): Result<Boolean> {
    return withContext(Dispatchers.IO) {
        try {
            val clipboardDir = java.io.File(context.cacheDir, "clipboard_images")

            if (clipboardDir.exists()) {
                // Our cache usage is already bad enough, lets clean up old clipboard images
                clipboardDir.listFiles()?.forEach { it.delete() }
            } else {
                clipboardDir.mkdirs()
            }

            val tempFile = java.io.File(clipboardDir, "clipboard_${System.currentTimeMillis()}.${image.fileFormat}")

            tempFile.outputStream().use { outputStream ->
                downloadToStream(image, outputStream).getOrThrow()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            val clip = ClipEntry(ClipData.newUri(context.contentResolver, "image", uri))
            clipboard.setClipEntry(clip)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


private fun getMimeType(fileFormat: String): String? {
    return when (fileFormat) {
        "jpeg", "jpg" -> "image/jpeg"
        "png"         -> "image/png"
        "gif"         -> "image/gif"
        "webp"        -> "image/webp"
        else -> null
    }
}
