package moe.apex.rule34.util

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import moe.apex.rule34.prefs


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
                    scope.launch { prefs.updateStorageLocation(selectedUri) }
                    println(selectedUri)
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
