package com.fancyfinery.mobile.features.cart.data.local

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * The bag, stored on the device.
 *
 * There is no server-side cart — the website keeps its bag in the browser and
 * only sends line references at checkout — so the app does the same thing with
 * Room. That is also what makes the bag survive being killed by the OS, which
 * on Android happens routinely rather than exceptionally.
 *
 * **Prices are cached for display only.** `unitPriceMinor` and `priceFormatted`
 * exist so the bag can render a total without refetching every product, and
 * nothing is ever charged from them: checkout sends only product id, variant id
 * and quantity, and the server re-prices the whole basket from the catalogue.
 * A tampered row here changes what the customer sees in the bag and nothing
 * about what they pay.
 *
 * The primary key is a composite of product and variant, so adding the same
 * dress in two sizes makes two lines while adding the same size twice increases
 * the quantity of one.
 */
@Entity(
    tableName = "cart_items",
    primaryKeys = ["productId", "variantId"],
    indices = [Index(value = ["addedAt"])],
)
data class CartItemEntity(
    val productId: String,
    /**
     * Empty string rather than null for a product with no variants: SQLite
     * does not allow a NULL inside a composite primary key, and a row that
     * cannot be written is a line that silently never reaches the bag.
     */
    val variantId: String,
    val slug: String,
    val name: String,
    val size: String?,
    val color: String?,
    val imageUrl: String?,
    /** Cached for display. Never used to charge. */
    val unitPriceMinor: Long,
    val priceFormatted: String,
    val currency: String,
    val qty: Int,
    /** Stock at the time it was added — a hint for the UI, re-checked server-side. */
    val stockQty: Int,
    val addedAt: Long,
)

@Dao
interface CartDao {

    /** Newest first, so a piece just added is at the top of the bag. */
    @Query("SELECT * FROM cart_items ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items ORDER BY addedAt DESC")
    suspend fun all(): List<CartItemEntity>

    @Query("SELECT COALESCE(SUM(qty), 0) FROM cart_items")
    fun observeCount(): Flow<Int>

    @Query(
        "SELECT * FROM cart_items WHERE productId = :productId AND variantId = :variantId LIMIT 1",
    )
    suspend fun find(productId: String, variantId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query(
        "UPDATE cart_items SET qty = :qty WHERE productId = :productId AND variantId = :variantId",
    )
    suspend fun setQty(productId: String, variantId: String, qty: Int)

    @Query("DELETE FROM cart_items WHERE productId = :productId AND variantId = :variantId")
    suspend fun remove(productId: String, variantId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    /**
     * Drop every cached price.
     *
     * Called when the shopper changes currency: the stored figures are now in
     * the wrong one, and showing a naira total under a dollar sign is worse
     * than showing nothing. The bag is re-priced from the catalogue on next
     * open.
     */
    @Query("UPDATE cart_items SET priceFormatted = '', currency = :currency")
    suspend fun invalidatePrices(currency: String)
}
