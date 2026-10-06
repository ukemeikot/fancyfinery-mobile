package com.fancyfinery.mobile.features.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.core.session.SessionStore
import com.fancyfinery.mobile.features.account.data.CheckoutLineRequest
import com.fancyfinery.mobile.features.account.data.QuoteRequest
import com.fancyfinery.mobile.features.cart.data.CartRepository
import com.fancyfinery.mobile.features.cart.data.local.CartItemEntity
import com.fancyfinery.mobile.features.checkout.data.CheckoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartState(
    val items: List<CartItemEntity> = emptyList(),
    val isLoading: Boolean = true,
    /** Server-priced subtotal, once a quote has been fetched. */
    val subtotalFormatted: String? = null,
    val isPricing: Boolean = false,
    val error: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
    val count: Int get() = items.sumOf { it.qty }
}

/**
 * The bag.
 *
 * Shows the lines from the device, and asks the server for the subtotal rather
 * than adding up the cached prices itself. That is not pedantry: the cached
 * figures were captured when each piece was added, and a price change or a
 * currency switch between then and now would otherwise leave the bag confidently
 * displaying a total the checkout will not honour.
 *
 * The quote is requested against Nigeria purely to obtain a priced subtotal —
 * postage needs a real destination and is not shown here, since the customer
 * has not given an address yet.
 */
class CartViewModel(
    private val cart: CartRepository,
    private val checkout: CheckoutRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            cart.observeItems().collect { items ->
                _state.update { it.copy(items = items, isLoading = false) }
                refreshSubtotal(items)
            }
        }
    }

    fun onIncrease(item: CartItemEntity) {
        viewModelScope.launch {
            cart.setQty(item.productId, item.variantId, item.qty + 1)
        }
    }

    fun onDecrease(item: CartItemEntity) {
        viewModelScope.launch {
            cart.setQty(item.productId, item.variantId, item.qty - 1)
        }
    }

    fun onRemove(item: CartItemEntity) {
        viewModelScope.launch { cart.remove(item.productId, item.variantId) }
    }

    fun onClear() {
        viewModelScope.launch { cart.clear() }
    }

    /**
     * Ask the server what the items actually cost right now.
     *
     * Failure is deliberately quiet: the bag still lists what is in it, and the
     * subtotal line simply does not appear. Replacing the whole screen with an
     * error because a price lookup failed would hide the customer's own basket
     * from them.
     */
    private fun refreshSubtotal(items: List<CartItemEntity>) {
        if (items.isEmpty()) {
            _state.update { it.copy(subtotalFormatted = null) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isPricing = true) }
            runCatching {
                checkout.quote(
                    QuoteRequest(
                        // A destination is required to price anything; the house
                        // is Nigerian and this figure is the items only.
                        countryCode = "NG",
                        items = items.map {
                            CheckoutLineRequest(
                                productId = it.productId,
                                variantId = it.variantId.takeIf { v -> v.isNotEmpty() },
                                qty = it.qty,
                            )
                        },
                    ),
                )
            }
                .onSuccess { quote ->
                    _state.update {
                        it.copy(
                            isPricing = false,
                            subtotalFormatted = quote.breakdown.subtotal.formatted,
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isPricing = false, subtotalFormatted = null) }
                }
        }
    }
}
