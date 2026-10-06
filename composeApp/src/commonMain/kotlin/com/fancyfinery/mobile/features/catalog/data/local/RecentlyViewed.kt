package com.fancyfinery.mobile.features.catalog.data.local

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * Recently viewed pieces — the app's version of `RecentlyViewedProvider`.
 *
 * Local, like the website's, because it is a browsing convenience rather than
 * account data: it should work signed out, and nobody needs their browsing
 * history synced to a server to be shown a row of things they just looked at.
 *
 * Keyed by slug — the same key the catalogue and the website's URLs use — so a
 * row survives a product being re-indexed.
 */
@Entity(tableName = "recently_viewed")
data class RecentlyViewedEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val imageUrl: String?,
    val priceFormatted: String,
    val viewedAt: Long,
)

@Dao
interface RecentlyViewedDao {

    @Query("SELECT * FROM recently_viewed ORDER BY viewedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 12): Flow<List<RecentlyViewedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(entry: RecentlyViewedEntity)

    /**
     * Keep the list short.
     *
     * Without this the table grows for the life of the install, and the row the
     * customer sees is the same twelve items regardless. Trimming on write is
     * cheaper than a scheduled clean-up and has no moving parts.
     */
    @Query(
        """
        DELETE FROM recently_viewed
        WHERE slug NOT IN (
            SELECT slug FROM recently_viewed ORDER BY viewedAt DESC LIMIT :keep
        )
        """,
    )
    suspend fun trim(keep: Int = 24)

    @Query("DELETE FROM recently_viewed")
    suspend fun clear()
}
