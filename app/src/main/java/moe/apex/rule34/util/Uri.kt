package moe.apex.rule34.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri


fun launchUriWithPackage(context: Context, uri: Uri, packageName: String) {
    val launchIntent = createViewIntent(uri, packageName)
    context.startActivity(launchIntent)
}


fun launchInDefaultBrowser(context: Context, url: String) {
    launchInDefaultBrowser(context, url.toUri())
}


fun launchInDefaultBrowser(context: Context, uri: Uri) {
    val defaultBrowserIntent = createDefaultBrowserIntent()
    val defaultPackage = getDefaultPackageForIntent(context.packageManager, defaultBrowserIntent)
    defaultPackage?.let {
        launchUriWithPackage(context, uri, it)
    } ?: showToast(context, "No browser found to handle URI: $uri")
}
