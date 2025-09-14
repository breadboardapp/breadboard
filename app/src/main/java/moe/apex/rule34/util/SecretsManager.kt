package moe.apex.rule34.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import moe.apex.rule34.BuildConfig
import java.security.MessageDigest
import java.util.Base64
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

            val secretKeyBytes = Base64.getDecoder().decode(signatureHash)
            val secretKey = SecretKeySpec(secretKeyBytes.copyOf(16), "AES")

            val encryptedData = Base64.getDecoder().decode(encryptedApiKey)

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
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo!!.apkContentsSigners
            } else {
                packageInfo.signatures
            }

            val signature = signatures!!.first().toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signature)
            Base64.getEncoder().encodeToString(digest)
        } catch (e: Exception) {
            null
        }
    }
}