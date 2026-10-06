package com.fancyfinery.mobile.core.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun createDatabase(context: Any?): AppDatabase {
    val dbPath = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )!!.path + "/$APP_DATABASE_NAME"

    return Room.databaseBuilder<AppDatabase>(name = dbPath)
        .setDriver(driver = BundledSQLiteDriver())
        .build()
}
