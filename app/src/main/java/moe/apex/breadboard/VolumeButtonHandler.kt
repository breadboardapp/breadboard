package moe.apex.breadboard

import android.view.KeyEvent


interface VolumeButtonHandler {
    var volumeUpPressedCallback: (() -> Boolean)?


    fun handleVolumeUpKey(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return volumeUpPressedCallback?.invoke() ?: false
        }
        return false
    }
}

