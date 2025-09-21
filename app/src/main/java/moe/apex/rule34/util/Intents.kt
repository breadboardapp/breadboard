package moe.apex.rule34.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri


fun getDefaultPackageForIntent(packageManager: PackageManager, intent: Intent): String? {
    val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName
}


fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = url.toUri()
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TOP
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        showToast(context, "Unable to open link.")
    }
}


fun createViewIntent(uri: Uri, targetPackage: String? = null): Intent {
    return Intent(Intent.ACTION_VIEW, uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        targetPackage?.let { setPackage(it) }
    }
}


fun createWebBrowserIntent(): Intent {
    return Intent(Intent.ACTION_VIEW, "http://example.com".toUri())
}
