package moe.apex.breadboard.util

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import moe.apex.breadboard.BuildConfig
import moe.apex.breadboard.preferences.PreferenceKeys
import moe.apex.breadboard.preferences.Prefs
import moe.apex.breadboard.preferences.UserPreferencesRepository
import moe.apex.breadboard.prefs


enum class ReleasePlatform(val displayName: String) {
    DEBUG("Debug"),
    PLAY_STORE("Play Store"),
    UNKNOWN("Unknown");
}


// Ignore the warning about a when branch being unreachable
val releasePlatform = when (BuildConfig.APPLICATION_ID) {
    "moe.apex.breadboard" -> ReleasePlatform.PLAY_STORE
    "moe.apex.breadboard.debug" -> ReleasePlatform.DEBUG
    else -> ReleasePlatform.UNKNOWN
}


object AgeVerification {
    fun hasVerifiedAge(prefs: Prefs): Boolean {
        return prefs.getInternalAgeVerificationStatus()
    }


    private suspend fun setVerifiedAge(preferencesRepository: UserPreferencesRepository) {
        preferencesRepository.updatePref(PreferenceKeys.HAS_VERIFIED_AGE, true)
    }


    @Composable
    fun AgeVerifyDialog(
        onDismissRequest: () -> Unit,
        onAgeVerified: () -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        val preferencesRepository = LocalContext.current.prefs

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Age verification") },
            text = {
                Text(
                    text = "Breadboard is an app that displays content from other platforms and " +
                           "cannot guarantee that all content is suitable for all ages.\n\n" +
                           "This action is unavailable until you have confirmed you are at least " +
                           "18 years old or the legal age in your country to view adult content."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { setVerifiedAge(preferencesRepository) }.invokeOnCompletion {
                            onAgeVerified()
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        )
    }
}
