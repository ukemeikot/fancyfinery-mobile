package com.fancyfinery.mobile.features.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.features.account.data.AccountRepository
import com.fancyfinery.mobile.features.account.data.OrderDetailDto
import com.fancyfinery.mobile.features.checkout.data.CheckoutRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderDetailState(
    val isLoading: Boolean = true,
    val order: OrderDetailDto? = null,
    val isBusy: Boolean = false,
    val notice: String? = null,
    val error: String? = null,
)

class OrderDetailViewModel(
    private val orderId: String,
    private val account: AccountRepository,
    private val checkout: CheckoutRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OrderDetailState())
    val state: StateFlow<OrderDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { account.order(orderId).order }
                .onSuccess { order ->
                    _state.update { it.copy(isLoading = false, order = order) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = (e as? ApiException)?.message ?: "Could not load this order.",
                        )
                    }
                }
        }
    }

    fun onPay(openUrl: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, notice = null) }
            runCatching { checkout.startPayment(orderId) }
                .onSuccess { payment ->
                    _state.update { it.copy(isBusy = false) }
                    openUrl(payment.paymentUrl)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isBusy = false,
                            notice = (e as? ApiException)?.message
                                ?: "Could not start payment.",
                        )
                    }
                }
        }
    }

    /**
     * Poll until the charge clears, or give up quietly.
     *
     * Called when the customer returns from the payment page. Settlement is
     * confirmed by the provider's webhook with a nightly reconcile behind it, so
     * giving up here does NOT mean the payment failed — it means the screen
     * stopped waiting. The order is reloaded either way.
     */
    fun onReturnedFromPayment() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            repeat(POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                val status = runCatching { checkout.paymentStatus(orderId) }.getOrNull()
                if (status?.paymentStatus == "paid") {
                    _state.update { it.copy(isBusy = false) }
                    load()
                    return@launch
                }
            }
            _state.update { it.copy(isBusy = false) }
            load()
        }
    }

    fun onCancel() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, notice = null) }
            runCatching { account.cancelOrder(orderId) }
                .onSuccess { cancelled ->
                    _state.update { it.copy(isBusy = false) }
                    if (cancelled) load() else _state.update {
                        it.copy(notice = "This order can no longer be cancelled.")
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isBusy = false,
                            // The server's wording explains WHY — already paid,
                            // already shipped — which is what the customer needs.
                            notice = (e as? ApiException)?.message
                                ?: "Could not cancel this order.",
                        )
                    }
                }
        }
    }

    private companion object {
        const val POLL_ATTEMPTS = 10
        const val POLL_INTERVAL_MS = 2_000L
    }
}
