package com.fancyfinery.mobile.features.product.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.features.cart.data.CartRepository
import com.fancyfinery.mobile.features.catalog.data.CatalogRepository
import com.fancyfinery.mobile.features.catalog.data.local.RecentlyViewedDao
import com.fancyfinery.mobile.features.catalog.data.local.RecentlyViewedEntity
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductDetailDto
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ReviewDto
import com.fancyfinery.mobile.features.catalog.data.remote.dto.VariantDto
import com.fancyfinery.mobile.features.wishlist.data.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class ProductState(
    val isLoading: Boolean = true,
    val product: ProductDetailDto? = null,
    val reviews: List<ReviewDto> = emptyList(),
    val selectedVariant: VariantDto? = null,
    val qty: Int = 1,
    val isSaved: Boolean = false,
    val isAdding: Boolean = false,
    val error: String? = null,
    /** Transient confirmation, e.g. "Added to your bag". */
    val toast: String? = null,
) {
    /**
     * Sizes offered, in the order the house lists them.
     *
     * Distinct because a product may carry the same size in several colours,
     * and the picker should show each size once.
     */
    val sizes: List<String>
        get() = product?.variants?.mapNotNull { it.size }?.distinct().orEmpty()

    val colors: List<String>
        get() = product?.variants?.mapNotNull { it.color }?.distinct().orEmpty()

    /**
     * Whether the bag button should be enabled.
     *
     * A product with variants needs one chosen first — adding "a dress" when
     * the shop sells four sizes of it is not an order anyone can fulfil.
     */
    val canAddToBag: Boolean
        get() {
            val p = product ?: return false
            if (!p.inStock) return false
            if (p.variants.isEmpty()) return true
            return selectedVariant?.inStock == true
        }
}

/**
 * Drives the product screen.
 *
 * Records the view locally as a side effect of loading, which is what feeds the
 * "recently viewed" row — the same thing `TrackView` does on the website, and
 * for the same reason: it belongs to the browsing session, not to an account,
 * so it must work signed out.
 */
class ProductViewModel(
    private val slug: String,
    private val catalog: CatalogRepository,
    private val cart: CartRepository,
    private val wishlist: WishlistRepository,
    private val recentlyViewed: RecentlyViewedDao,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductState())
    val state: StateFlow<ProductState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            runCatching { catalog.product(slug) }
                .onSuccess { response ->
                    val product = response.product
                    _state.update {
                        it.copy(
                            isLoading = false,
                            product = product,
                            reviews = response.reviews,
                            // Preselect when there is only one real choice, so
                            // a single-size piece is one tap to buy.
                            selectedVariant = product.variants
                                .takeIf { v -> v.size == 1 }
                                ?.firstOrNull(),
                        )
                    }
                    recordView(product)
                    refreshSaved(product.id)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = (e as? ApiException)?.message
                                ?: "Could not load this piece.",
                        )
                    }
                }
        }
    }

    fun onSelectSize(size: String) {
        val product = _state.value.product ?: return
        val color = _state.value.selectedVariant?.color
        // Prefer a variant that also matches the colour already chosen, so
        // picking a size does not silently change the colour.
        val match = product.variants.firstOrNull { it.size == size && it.color == color }
            ?: product.variants.firstOrNull { it.size == size }
        _state.update { it.copy(selectedVariant = match, qty = 1) }
    }

    fun onSelectColor(color: String) {
        val product = _state.value.product ?: return
        val size = _state.value.selectedVariant?.size
        val match = product.variants.firstOrNull { it.color == color && it.size == size }
            ?: product.variants.firstOrNull { it.color == color }
        _state.update { it.copy(selectedVariant = match, qty = 1) }
    }

    fun onQtyChange(delta: Int) {
        _state.update { current ->
            val max = current.selectedVariant?.stockQty ?: MAX_PER_LINE
            current.copy(qty = (current.qty + delta).coerceIn(1, maxOf(1, minOf(max, MAX_PER_LINE))))
        }
    }

    fun onAddToBag() {
        val current = _state.value
        val product = current.product ?: return

        viewModelScope.launch {
            _state.update { it.copy(isAdding = true) }
            val added = cart.add(
                product = product,
                variant = current.selectedVariant,
                qty = current.qty,
            )
            _state.update {
                it.copy(
                    isAdding = false,
                    toast = if (added) {
                        "Added to your bag"
                    } else {
                        "Only ${current.selectedVariant?.stockQty ?: 0} left — adjust the quantity."
                    },
                )
            }
        }
    }

    /**
     * Save or unsave.
     *
     * Requires an account: the wishlist is stored server-side so it follows the
     * customer between the app and the website. [onNeedsAuth] lets the screen
     * send them to sign-in rather than failing silently.
     */
    fun onToggleSaved(onNeedsAuth: () -> Unit) {
        val product = _state.value.product ?: return
        viewModelScope.launch {
            val result = wishlist.toggle(product.id, _state.value.isSaved)
            when (result) {
                WishlistRepository.ToggleResult.NeedsAuth -> onNeedsAuth()
                WishlistRepository.ToggleResult.Saved ->
                    _state.update { it.copy(isSaved = true, toast = "Saved") }
                WishlistRepository.ToggleResult.Removed ->
                    _state.update { it.copy(isSaved = false, toast = "Removed from saved") }
                WishlistRepository.ToggleResult.Failed ->
                    _state.update { it.copy(toast = "Couldn't update your saved list.") }
            }
        }
    }

    fun dismissToast() = _state.update { it.copy(toast = null) }

    private suspend fun refreshSaved(productId: String) {
        val saved = wishlist.isSaved(productId)
        _state.update { it.copy(isSaved = saved) }
    }

    private suspend fun recordView(product: ProductDetailDto) {
        runCatching {
            recentlyViewed.record(
                RecentlyViewedEntity(
                    slug = product.slug,
                    name = product.name,
                    imageUrl = product.images.firstOrNull { !it.isVideo }?.url,
                    priceFormatted = product.price.formatted,
                    viewedAt = Clock.System.now().toEpochMilliseconds(),
                ),
            )
            recentlyViewed.trim()
        }
    }

    private companion object {
        /** Matches the server's per-line cap in `checkoutSchema`. */
        const val MAX_PER_LINE = 99
    }
}
