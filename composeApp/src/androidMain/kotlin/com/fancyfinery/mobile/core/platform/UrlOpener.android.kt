package com.fancyfinery.mobile.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/**
 * Chrome Custom Tabs.
 *
 * Falls back to a plain browser Intent when no Custom Tabs provider is
 * installed — which does happen on devices without Chrome, and on some
 * manufacturer ROMs. Without the fallback, Google sign-in and payment would
 * both simply do nothing on those handsets, with no error to explain it.
 */
actual class UrlOpener(private val context: Context) {
    actual fun open(url: String) {
        val uri: Uri = url.toUri()
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .also { it.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                .launchUrl(context, uri)
        }.onFailure {
            val fallback = Intent(Intent.ACTION_VIEW, uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(fallback) }
        }
    }
}

actual fun createUrlOpener(context: Any?): UrlOpener =
    UrlOpener(
        context as? Context
            ?: error("An Android Context is required to open a browser tab."),
    )
