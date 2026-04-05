package moe.apex.breadboard.util

import android.annotation.SuppressLint
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.apex.breadboard.preferences.FlagSecureMode
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.viewmodel.BreadboardViewModel
import moe.apex.breadboard.viewmodel.GlobalViewModelOwner


class FlagSecureHelper {
    companion object {
        @SuppressLint("ComposableNaming")
        @Composable
        /** Start listening for changes to prefs or incognito to determine
            whether or not [WindowManager.LayoutParams.FLAG_SECURE] should be enabled. */
        fun register() {
            val viewModel: BreadboardViewModel = viewModel(GlobalViewModelOwner)
            val incognito by viewModel.incognito.collectAsState()
            val prefs = LocalPreferences.current
            val window = (LocalActivity.current)?.window

            fun enableFlagSecure() {
                window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            fun disableFlagSecure() {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            LaunchedEffect(prefs.flagSecureMode, incognito) {
                when (prefs.flagSecureMode) {
                    FlagSecureMode.ON -> enableFlagSecure()
                    FlagSecureMode.OFF -> disableFlagSecure()
                    FlagSecureMode.AUTO -> if (incognito) {
                        enableFlagSecure()
                    } else {
                        disableFlagSecure()
                    }
                }
            }
        }
    }
}
