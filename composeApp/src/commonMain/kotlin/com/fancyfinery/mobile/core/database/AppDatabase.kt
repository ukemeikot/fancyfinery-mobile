package com.fancyfinery.mobile.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.fancyfinery.mobile.features.auth.data.local.UserDao
import com.fancyfinery.mobile.features.auth.data.local.UserEntity
import com.fancyfinery.mobile.features.cart.data.local.CartDao
import com.fancyfinery.mobile.features.cart.data.local.CartItemEntity
import com.fancyfinery.mobile.features.catalog.data.local.RecentlyViewedDao
import com.fancyfinery.mobile.features.catalog.data.local.RecentlyViewedEntity

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * The device's local store.
 *
 * Holds only what the app genuinely needs offline or between launches: the
 * signed-in user, the bag, and recently-viewed pieces. Everything else is read
 * from the server, because a stale catalogue is worse than a slow one — a
 * cached price that no longer matches the shop is the one thing a storefront
 * must never show.
 */
@Database(
    entities = [
        UserEntity::class,
        CartItemEntity::class,
        RecentlyViewedEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun cartDao(): CartDao
    abstract fun recentlyViewedDao(): RecentlyViewedDao
}

const val APP_DATABASE_NAME = "fancyfinery.db"
