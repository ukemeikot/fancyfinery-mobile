package com.fancyfinery.mobile.core.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

/**
 * Desktop is the DEVELOPMENT PREVIEW host, not a shipping target.
 *
 * It exists so Compose Hot Reload has a JVM process to run in — hot reload does
 * not work on Android or iOS, and a live window is worth more during UI work
 * than an emulator rebuild per change.
 *
 * The database therefore lives under a dot-directory in the developer's home,
 * and deleting it is always safe: nothing here is a customer's data.
 */
actual fun createDatabase(context: Any?): AppDatabase {
    val appDir = File(System.getProperty("user.home"), ".fancyfinery-preview")
        .also { it.mkdirs() }
    return Room.databaseBuilder<AppDatabase>(
        File(appDir, APP_DATABASE_NAME).absolutePath,
    ).setDriver(driver = BundledSQLiteDriver()).build()
}
