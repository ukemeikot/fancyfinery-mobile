package com.fancyfinery.mobile.features.cart.data

import com.fancyfinery.mobile.core.session.SessionStore
import com.fancyfinery.mobile.features.cart.data.local.CartDao
import com.fancyfinery.mobile.features.cart.data.local.CartItemEntity
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductDetailDto
import com.fancyfinery.mobile.features.catalog.data.remote.dto.VariantDto
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

/**
 * The bag.
 *
 * Mirrors the website's `CartProvider`: lines live on the device, and only
 * references travel at checkout. Everything money-shaped here is for display.
 *
 * The one rule worth stating plainly, because getting it wrong is how a shop
 * oversells: **stock is a hint, not a control**. `stockQty` is whatever the
 * catalogue said when the piece was added, and it is used to stop the obvious
 * mistake of adding a tenth of something with three left. The real decision is
 * a conditional decrement inside the order transaction server-side, which is
 * the only thing that can arbitrate between two customers reaching for the last
 * one.
 */
class CartRepository(
    private val dao: CartDao,
    private val session: SessionStore,
) {

    fun observeItems(): Flow<List<CartItemEntity>> = dao.observeAll()

    /** Total pieces in the bag, for the tab badge. */
    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun items(): List<CartItemEntity> = dao.all()

    /**
     * Add a piece, or increase the quantity if that exact line is already in
     * the bag.
     *
     * Returns false when the requested quantity exceeds what the catalogue said
     * was in stock, so the caller can say why rather than silently capping.
     */
    suspend fun add(
        product: ProductDetailDto,
        variant: VariantDto?,
        qty: Int = 1,
    ): Boolean {
        val variantId = variant?.id.orEmpty()
        val existing = dao.find(productId = product.id, variantId = variantId)
        val desired = (existing?.qty ?: 0) + qty

        val available = variant?.stockQty
            // A product with no variants carries its stock on the variants it
            // does not have, so treat it as available and let the server decide.
            ?: product.variants.sumOf { it.stockQty }.takeIf { product.variants.isNotEmpty() }
            ?: Int.MAX_VALUE

        if (desired > available) return false

        dao.upsert(
            CartItemEntity(
                productId = product.id,
                variantId = variantId,
                slug = product.slug,
                name = product.name,
                size = variant?.size,
                color = variant?.color,
                imageUrl = product.images.firstOrNull { !it.isVideo }?.url,
                unitPriceMinor = product.price.amount,
                priceFormatted = product.price.formatted,
                currency = product.price.currency,
                qty = desired,
                stockQty = available,
                addedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
        return true
    }

    /** Set an exact quantity. Zero or less removes the line. */
    suspend fun setQty(productId: String, variantId: String, qty: Int) {
        if (qty <= 0) {
            dao.remove(productId = productId, variantId = variantId)
        } else {
            dao.setQty(productId = productId, variantId = variantId, qty = qty)
        }
    }

    suspend fun remove(productId: String, variantId: String) =
        dao.remove(productId = productId, variantId = variantId)

    /** Empty the bag. Called once an order has actually been placed. */
    suspend fun clear() = dao.clear()

    /**
     * The lines as checkout wants them: ids and quantities, nothing else.
     *
     * This is the whole contract with the server. Names, prices and stock are
     * all recomputed there from the catalogue, so a tampered local bag cannot
     * change what is charged.
     */
    suspend fun checkoutLines(): List<CheckoutLine> = dao.all().map { item ->
        CheckoutLine(
            productId = item.productId,
            variantId = item.variantId.takeIf { it.isNotEmpty() },
            qty = item.qty,
        )
    }

    /** Discard cached prices after a currency change. */
    suspend fun onCurrencyChanged() = dao.invalidatePrices(session.currency.value)
}

/** One line as the checkout and quote endpoints expect it. */
data class CheckoutLine(
    val productId: String,
    val variantId: String?,
    val qty: Int,
)
