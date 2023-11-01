package me.devoxin.rule34.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

abstract class AuthenticatableActivity : AppCompatActivity() {
    private var authenticating = false

    private var switchingActivity = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val appSecured = preferences.getBoolean(SettingsActivity.SETTING_SECURED, false)

        if (appSecured) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun switchActivity(intent: Intent) {
        switchingActivity = true
        startActivity(intent)
    }

    override fun onStop() {
        super.onStop()

        if (!isFinishing && !switchingActivity && !authenticating) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            val appSecured = preferences.getBoolean(SettingsActivity.SETTING_SECURED, false)

            if (appSecured) {
                authenticating = true

                val intent = Intent(this, AuthenticateActivity::class.java)
                intent.putExtra("lastActivity", true)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!switchingActivity) {
            if (authenticating) {
                authenticating = false
            }
        } else {
            switchingActivity = false
        }
    }
}
