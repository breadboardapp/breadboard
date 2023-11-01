package me.devoxin.rule34.activities

import android.content.Intent
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
import android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class AuthenticateActivity : AppCompatActivity() {
    private var lastActivity = false

    private var backgrounded = false
    private var finished = false
    private var authenticated = false

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


        onBackPressedDispatcher.addCallback {
            minimise()
        }
    }

    private fun minimise() {
        if (lastActivity) {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            startActivity(intent)
        }
    }

    override fun onStop() {
        super.onStop()

        if (!backgrounded && !finished) {
            backgrounded = true
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

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                when (errorCode) {
                    BIOMETRIC_ERROR_USER_CANCELED -> minimise()
                    else -> Toast.makeText(this@AuthenticateActivity, "Authentication failed with error code $errorCode", Toast.LENGTH_SHORT).show()
                }
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
