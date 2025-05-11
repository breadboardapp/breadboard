package moe.apex.rule34.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
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
    scope.launch(Dispatchers.IO) { context.prefs.updateStorageLocation(tree) }
}


@Composable
fun StorageLocationSelection(promptType: PromptType, onFailure: () -> Unit, onSuccess: (Uri) -> Unit) {
    val context = LocalContext.current

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedUri = result.data?.data
                if (selectedUri != null) {
                    onSuccess(selectedUri)
                }
            } else {
                showToast(context, "Nothing selected.")
            }
            onFailure()
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
