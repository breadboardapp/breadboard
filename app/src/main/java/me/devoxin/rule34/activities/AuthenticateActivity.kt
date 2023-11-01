package me.devoxin.rule34.activities

import android.content.Intent
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class AuthenticateActivity : AppCompatActivity() {
    private var backgrounded = false
    private var finished = false

    private var lastActivity = false

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastActivity = intent.getBooleanExtra("lastActivity", false)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val requireAuthentication = preferences.getBoolean(SettingsActivity.SETTING_SECURED, false)

        if (!requireAuthentication) {
            openApplication()
        } else {
            authenticate()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()

        if (backgrounded) {
            backgrounded = false

            authenticate()
        }
    }

    override fun onStop() {
        super.onStop()

        if (!backgrounded && !finished) {
            backgrounded = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun authenticate() {
        val biometricPrompt = BiometricPrompt.Builder(this)
            .setTitle("Authentication Required")
            .setSubtitle("This application requires authentication before it can be accessed.")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(CancellationSignal(), mainExecutor, object : AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                openApplication()
            }
        })
    }

    fun openApplication() {
        if (!lastActivity) {
            startActivity(Intent(this, MainActivity::class.java))
        }

        finished = true
        finish()
    }
}
