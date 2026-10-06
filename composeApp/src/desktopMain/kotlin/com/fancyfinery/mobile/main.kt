package com.fancyfinery.mobile

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * The development preview host.
 *
 * This is NOT a product target — Fancy Finery ships to Android and iOS. It
 * exists because Compose Hot Reload runs only on the JVM, and a window that
 * redraws on save is worth far more during UI work than a Gradle install and an
 * emulator restart for every spacing change.
 *
 * The window is deliberately 400x800: close to a phone's aspect ratio, so a
 * layout that looks right here is not quietly relying on desktop width. It is
 * still a preview rather than a simulator — touch targets, system bars, safe
 * areas and real network conditions all need a device before shipping.
 *
 * Run it with:  ./gradlew :composeApp:runHot --auto
 */
fun main() = application {
    val windowState = rememberWindowState(size = DpSize(width = 400.dp, height = 800.dp))
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Fancy Finery — preview",
        content = { App() },
    )
}
