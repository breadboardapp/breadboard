package moe.apex.rule34.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri


fun launchUriWithPackage(context: Context, uri: Uri, packageName: String?) {
    val launchIntent = createViewIntent(uri, packageName)
    context.startActivity(launchIntent)
}


fun launchInWebBrowser(context: Context, url: String) {
    launchInWebBrowser(context, url.toUri())
}


fun launchInWebBrowser(context: Context, uri: Uri) {
    val webBrowserIntent = createWebBrowserIntent()
    var defaultPackage = getDefaultPackageForIntent(context.packageManager, webBrowserIntent)

    defaultPackage?.let {
        /* Older Android versions allow the user to not have a default browser set, even if they do
           have browsers installed. In such cases, the "Open with" chooser will be opened.
           This apparently uses the package name "android", but if we try and launch that package,
           it won't work.
           Instead, we'll set the package to null and let the chooser open naturally. */
        if (it == "android") {
            defaultPackage = null
        }
    } ?: return showToast(context, "No browser found")

    launchUriWithPackage(context, uri, defaultPackage)
}
