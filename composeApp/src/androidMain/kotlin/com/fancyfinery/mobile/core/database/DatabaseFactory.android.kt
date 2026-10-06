package com.fancyfinery.mobile.core.database

import android.content.Context
import androidx.room3.Room

actual fun createDatabase(context: Any?): AppDatabase {
    requireNotNull(context) { "Android context required for Room database" }
    return Room.databaseBuilder(
        context = context as Context,
        klass = AppDatabase::class.java,
        name = APP_DATABASE_NAME,
    ).build()
}
