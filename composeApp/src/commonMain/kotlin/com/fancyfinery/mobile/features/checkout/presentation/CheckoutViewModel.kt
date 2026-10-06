package com.fancyfinery.mobile.features.checkout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.core.network.ApiErrorCode
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.core.session.SessionStore
import com.fancyfinery.mobile.features.account.data.AccountRepository
import com.fancyfinery.mobile.features.account.data.CheckoutLineRequest
import com.fancyfinery.mobile.features.account.data.CountryDto
import com.fancyfinery.mobile.features.account.data.NgAreaDto
import com.fancyfinery.mobile.features.account.data.NgStateDto
import com.fancyfinery.mobile.features.account.data.PlaceOrderRequest
import com.fancyfinery.mobile.features.account.data.QuoteRequest
import com.fancyfinery.mobile.features.account.data.QuoteResponse
import com.fancyfinery.mobile.features.cart.data.CartRepository
import com.fancyfinery.mobile.features.checkout.data.CheckoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutState(
    val isLoading: Boolean = true,
    val needsAuth: Boolean = false,

    // Address
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val apartment: String = "",
    val city: String = "",
    val stateProvince: String = "",
    val postal: String = "",
    val countryCode: String = "NG",
    val countryName: String = "Nigeria",

    val countries: List<CountryDto> = emptyList(),

    // Nigeria local delivery
    val ngStates: List<NgStateDto> = emptyList(),
    val ngAreas: List<NgAreaDto> = emptyList(),
    val selectedNgStateId: String? = null,
    val selectedNgAreaId: String? = null,

    val couponCode: String = "",
    val quote: QuoteResponse? = null,
    val isQuoting: Boolean = false,

    val isPlacing: Boolean = false,
    val fieldError: String? = null,
    val error: String? = null,
) {
    val isNigeria: Boolean get() = countryCode.equals("NG", ignoreCase = true)

    /** The engine has nothing to offer for this destination. */
    val unavailableReason: String?
        get() = when (quote?.unavailable) {
            "over-max-weight" -> "This bag is heavier than we can ship to that destination."
            "no-zone" -> "We don't ship to that destination yet."
            "no-rate" -> "No delivery rate is set for that destination yet."
            else -> null
        }

    val canPlace: Boolean
        get() = !isPlacing &&
            quote != null &&
            unavailableReason == null &&
            name.isNotBlank() &&
            email.contains('@') &&
            phone.isNotBlank() &&
            address.isNotBlank() &&
            city.isNotBlank() &&
            stateProvince.isNotBlank() &&
            postal.isNotBlank()
}

/** What the screen should do once the order exists. */
sealed interface CheckoutOutcome {
    /** Open this URL in a browser tab, then poll. */
    data class Pay(val orderId: String, val url: String) : CheckoutOutcome
    /** Nothing to pay online — the order is confirmed as pay-on-delivery. */
    data class PayOnDelivery(val orderId: String) : CheckoutOutcome
}

/**
 * Checkout.
 *
 * Every figure on this screen comes from `/shipping/quote`, and the order is
 * created from the same inputs that produced it. The app never adds anything
 * up: postage, tax, discounts and the Nigerian flat fee are all decided
 * server-side, which is what makes the total shown here the total charged.
 *
 * Re-quoting is debounced because it fires on every address keystroke that can
 * change a price — country, state, area, coupon — and a request per character
 * would both be slow and arrive out of order.
 */
class CheckoutViewModel(
    private val cart: CartRepository,
    private val checkout: CheckoutRepository,
    private val account: AccountRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutState())
    val state: StateFlow<CheckoutState> = _state.asStateFlow()

    private var quoteJob: Job? = null

    init {
        load()
    }

    fun load() {
        if (!session.isSignedIn) {
            _state.update { it.copy(isLoading = false, needsAuth = true) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, needsAuth = false) }

            val countries = runCatching { checkout.countries().items }.getOrDefault(emptyList())
            val ngStates = runCatching { checkout.nigerianStates().items }.getOrDefault(emptyList())

            // Prefill from the saved address — the whole reason checkout writes
            // it back after every order.
            val profile = runCatching { account.profile() }.getOrNull()
            val saved = profile?.address

            _state.update {
                it.copy(
                    isLoading = false,
                    countries = countries,
                    ngStates = ngStates,
                    name = profile?.fullName.orEmpty(),
                    email = profile?.email.orEmpty(),
                    phone = saved?.phone.orEmpty(),
                    address = saved?.address.orEmpty(),
                    city = saved?.city.orEmpty(),
                    stateProvince = saved?.state.orEmpty(),
                    countryName = saved?.country?.takeIf { c -> c.isNotBlank() } ?: "Nigeria",
                    countryCode = countries
                        .firstOrNull { c -> c.name.equals(saved?.country, ignoreCase = true) }
                        ?.code ?: "NG",
                )
            }
            requestQuote()
        }
    }

    // --- Field edits --------------------------------------------------------

    fun onName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPhone(v: String) = _state.update { it.copy(phone = v, error = null) }
    fun onAddress(v: String) = _state.update { it.copy(address = v, error = null) }
    fun onApartment(v: String) = _state.update { it.copy(apartment = v) }
    fun onCity(v: String) = _state.update { it.copy(city = v, error = null) }
    fun onStateProvince(v: String) = _state.update { it.copy(stateProvince = v, error = null) }
    fun onPostal(v: String) = _state.update { it.copy(postal = v, error = null) }

    fun onCountry(country: CountryDto) {
        _state.update {
            it.copy(
                countryCode = country.code,
                countryName = country.name,
                // Leaving Nigeria must drop the local-delivery selection, or the
                // order would carry an area id the destination cannot use.
                selectedNgStateId = null,
                selectedNgAreaId = null,
                ngAreas = emptyList(),
            )
        }
        requestQuote()
    }

    fun onNgState(stateId: String) {
        _state.update {
            it.copy(selectedNgStateId = stateId, selectedNgAreaId = null, ngAreas = emptyList())
        }
        viewModelScope.launch {
            val areas = runCatching { checkout.nigerianAreas(stateId).items }
                .getOrDefault(emptyList())
            _state.update { it.copy(ngAreas = areas) }
        }
    }

    fun onNgArea(areaId: String) {
        _state.update { it.copy(selectedNgAreaId = areaId) }
        requestQuote()
    }

    fun onCourier(courierId: String) {
        _state.update { current ->
            current.copy(quote = current.quote?.copy(selectedCourierId = courierId))
        }
        requestQuote()
    }

    fun onCoupon(v: String) {
        _state.update { it.copy(couponCode = v) }
        requestQuote()
    }

    // --- Quoting ------------------------------------------------------------

    private fun requestQuote() {
        quoteJob?.cancel()
        quoteJob = viewModelScope.launch {
            delay(QUOTE_DEBOUNCE_MS)
            val current = _state.value
            val lines = cart.checkoutLines()
            if (lines.isEmpty()) return@launch

            _state.update { it.copy(isQuoting = true) }
            runCatching {
                checkout.quote(
                    QuoteRequest(
                        countryCode = current.countryCode,
                        items = lines.map {
                            CheckoutLineRequest(it.productId, it.variantId, it.qty)
                        },
                        courierId = current.quote?.selectedCourierId,
                        ngDestinationId = current.selectedNgAreaId,
                        couponCode = current.couponCode.takeIf { c -> c.isNotBlank() },
                    ),
                )
            }
                .onSuccess { quote ->
                    _state.update { it.copy(isQuoting = false, quote = quote) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isQuoting = false,
                            error = (e as? ApiException)?.message
                                ?: "Could not calculate delivery.",
                        )
                    }
                }
        }
    }

    // --- Placing ------------------------------------------------------------

    fun onPlaceOrder(onOutcome: (CheckoutOutcome) -> Unit) {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isPlacing = true, error = null) }

            val lines = cart.checkoutLines()
            if (lines.isEmpty()) {
                _state.update { it.copy(isPlacing = false, error = "Your bag is empty.") }
                return@launch
            }

            runCatching {
                checkout.placeOrder(
                    PlaceOrderRequest(
                        name = current.name.trim(),
                        email = current.email.trim(),
                        phone = current.phone.trim(),
                        countryCode = current.countryCode,
                        country = current.countryName,
                        state = current.stateProvince.trim(),
                        city = current.city.trim(),
                        postal = current.postal.trim(),
                        address = current.address.trim(),
                        apartment = current.apartment.trim().takeIf { a -> a.isNotEmpty() },
                        courierId = current.quote?.selectedCourierId?.takeIf(::isRealCourier),
                        ngDestinationId = current.selectedNgAreaId,
                        couponCode = current.couponCode.takeIf { c -> c.isNotBlank() },
                        items = lines.map {
                            CheckoutLineRequest(it.productId, it.variantId, it.qty)
                        },
                    ),
                )
            }
                .onSuccess { placed ->
                    // The bag is only emptied once the order genuinely exists.
                    cart.clear()

                    if (!placed.requiresPayment) {
                        _state.update { it.copy(isPlacing = false) }
                        onOutcome(CheckoutOutcome.PayOnDelivery(placed.orderId))
                        return@onSuccess
                    }

                    runCatching { checkout.startPayment(placed.orderId) }
                        .onSuccess { payment ->
                            _state.update { it.copy(isPlacing = false) }
                            onOutcome(CheckoutOutcome.Pay(placed.orderId, payment.paymentUrl))
                        }
                        .onFailure {
                            // The order exists and is unpaid. Say so rather than
                            // implying it failed — it is payable from the order
                            // screen, and a second attempt here would duplicate it.
                            _state.update {
                                it.copy(
                                    isPlacing = false,
                                    error = "Your order was placed but payment couldn't start. " +
                                        "You can pay it from your orders.",
                                )
                            }
                            onOutcome(CheckoutOutcome.PayOnDelivery(placed.orderId))
                        }
                }
                .onFailure { e ->
                    val api = e as? ApiException
                    _state.update {
                        it.copy(
                            isPlacing = false,
                            needsAuth = api?.code == ApiErrorCode.Unauthenticated,
                            fieldError = api?.field,
                            error = api?.message ?: "Could not place your order.",
                        )
                    }
                }
        }
    }

    /**
     * Whether a courier id names an actual courier row.
     *
     * Nigerian local delivery is offered under the synthetic id
     * `ng-local-delivery`, which is deliberately NOT a uuid: it names something
     * that is not a courier. The server's checkout schema requires a uuid or
     * null, so sending the synthetic id would fail validation — the area is
     * already carried by `ngDestinationId`, which is what prices that line.
     */
    private fun isRealCourier(id: String): Boolean = UUID_PATTERN.matches(id)

    private companion object {
        const val QUOTE_DEBOUNCE_MS = 400L
        val UUID_PATTERN =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}
