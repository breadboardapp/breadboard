package moe.apex.breadboard.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import moe.apex.breadboard.BuildConfig
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


@Suppress("DEPRECATION")
object SecretsManager {
    private var decryptedApiKey: String? = null

    fun getApiKey(context: Context): String? {
        if (decryptedApiKey != null) {
            return decryptedApiKey
        }

        if (BuildConfig.DEBUG) {
            return BuildConfig.R34_API_KEY
        }

        try {
            val signatureHash = getSignatureHash(context)
                ?: throw SecurityException("Could not retrieve signature hash.")

            val encryptedApiKey = BuildConfig.R34_API_KEY
            if (encryptedApiKey.isEmpty()) {
                throw IllegalArgumentException("API key not found in BuildConfig.")
            }

            val secretKeyBytes = Base64.decode(signatureHash, Base64.NO_WRAP)
            val secretKey = SecretKeySpec(secretKeyBytes.copyOf(16), "AES")

            val encryptedData = Base64.decode(encryptedApiKey, Base64.NO_WRAP)

            val iv = encryptedData.copyOfRange(0, 12)
            val encryptedApiKeyBytes = encryptedData.copyOfRange(12, encryptedData.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmParameterSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
            val decryptedBytes = cipher.doFinal(encryptedApiKeyBytes)
            val decryptedKey = String(decryptedBytes)

            decryptedApiKey = decryptedKey
            return decryptedKey

        } catch (e: Exception) {
            Log.e("SecretsManager", "Failed to get R34 key.", e)
            return ""
        }
    }


    private fun getSignatureHash(context: Context): String? {
        return try {
            val signatureBytes = getSignatureBytes(context)
                ?: throw SecurityException("Failed to extract signature bytes from package")

            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signatureBytes)
            Base64.encodeToString(digest, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("SecretsManager", "Failed to get signature hash.", e)
            null
        }
    }


    private fun getSignatureBytes(context: Context): ByteArray? {
        return try {
            @Suppress("DEPRECATION")
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val flags = PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                context.packageManager.getPackageInfo(context.packageName, flags)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signature = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && packageInfo.signingInfo != null -> {
                    packageInfo.signingInfo!!.apkContentsSigners.firstOrNull()
                }
                else -> {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures?.firstOrNull()
                }
            }

            signature?.toByteArray()
        } catch (e: Exception) {
            Log.e("SecretsManager", "Failed to extract signature bytes.", e)
            null
        }
    }
}