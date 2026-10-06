package com.fancyfinery.mobile.core.platform

import java.awt.Desktop
import java.net.URI

/**
 * The system browser. Preview host only — this target does not ship.
 *
 * `Desktop` is unavailable on headless JVMs, so the call is guarded; the
 * preview must not die because a browser could not be launched.
 */
actual class UrlOpener {
    actual fun open(url: String) {
        runCatching {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI(url))
                }
            }
        }
    }
}

actual fun createUrlOpener(context: Any?): UrlOpener = UrlOpener()
