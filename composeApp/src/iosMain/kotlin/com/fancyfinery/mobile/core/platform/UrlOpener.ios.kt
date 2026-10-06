package com.fancyfinery.mobile.core.platform

import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication

/**
 * SFSafariViewController — iOS's equivalent of a Custom Tab.
 *
 * Presented over the key window's root view controller. That is the right
 * anchor rather than a view controller held by the caller, because this is
 * reached from shared Compose code, which has no UIKit controller of its own to
 * present from.
 *
 * If no window or root controller can be found — which happens if this is
 * somehow called before the scene is attached — it falls back to handing the
 * URL to the system, opening Safari proper. Leaving the customer on a dead
 * button would be the worse outcome.
 */
actual class UrlOpener {
    actual fun open(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return

        val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (root != null) {
            val safari = SFSafariViewController(uRL = nsUrl)
            // Present on whatever is already on top, so this still works when a
            // sheet or dialog is showing.
            val presenter = root.presentedViewController ?: root
            presenter.presentViewController(safari, animated = true, completion = null)
        } else {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
}

actual fun createUrlOpener(context: Any?): UrlOpener = UrlOpener()
