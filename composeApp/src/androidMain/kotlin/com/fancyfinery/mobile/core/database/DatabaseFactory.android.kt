package com.fancyfinery.mobile.core.database

import android.content.Context
import androidx.room3.Room

actual fun createDatabase(context: Any?): AppDatabase {
    requireNotNull(context) { "Android context required for Room database" }
    // applicationContext, deliberately: the app now passes its Activity so that
    // Credential Manager can show the Google sheet, and a database holding an
    // Activity would leak it on every rotation.
    return Room.databaseBuilder(
        context = (context as Context).applicationContext,
        klass = AppDatabase::class.java,
        name = APP_DATABASE_NAME,
    ).build()
}
