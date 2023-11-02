package me.devoxin.rule34.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.devoxin.rule34.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    companion object {
        const val SETTING_SECURED = "secured"
    }
}
