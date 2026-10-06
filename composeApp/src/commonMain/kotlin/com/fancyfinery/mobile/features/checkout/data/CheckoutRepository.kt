package com.fancyfinery.mobile.features.checkout.data

import com.fancyfinery.mobile.core.network.ApiClient
import com.fancyfinery.mobile.features.account.data.CountryListResponse
import com.fancyfinery.mobile.features.account.data.NgAreaListResponse
import com.fancyfinery.mobile.features.account.data.NgStateListResponse
import com.fancyfinery.mobile.features.account.data.OrderStatusResponse
import com.fancyfinery.mobile.features.account.data.PlaceOrderRequest
import com.fancyfinery.mobile.features.account.data.PlaceOrderResponse
import com.fancyfinery.mobile.features.account.data.QuoteRequest
import com.fancyfinery.mobile.features.account.data.QuoteResponse
import com.fancyfinery.mobile.features.account.data.StartPaymentResponse

/**
 * Checkout: destinations, pricing, placing the order and paying for it.
 *
 * The app sends ids and quantities; every figure comes back from the server.
 * Nothing here computes money — not the subtotal, not postage, not the Nigerian
 * flat fee — because the only total that matters is the one the order is
 * created with, and that is produced by `computeQuote` and `placeOrder` on the
 * other side of this boundary.
 */
class CheckoutRepository(private val api: ApiClient) {

    suspend fun countries(): CountryListResponse = api.get("/shipping/countries")

    suspend fun nigerianStates(): NgStateListResponse =
        api.get("/shipping/nigeria/states")

    /** Delivery areas for one state, each with its flat fee already converted. */
    suspend fun nigerianAreas(stateId: String): NgAreaListResponse =
        api.get("/shipping/nigeria/states/$stateId/areas")

    /**
     * Price the basket for a destination.
     *
     * The single source of truth for what the customer will be charged. The
     * product page, the bag and this screen all go through it, so no two
     * surfaces can show different money.
     */
    suspend fun quote(request: QuoteRequest): QuoteResponse =
        api.post("/shipping/quote", request)

    suspend fun placeOrder(request: PlaceOrderRequest): PlaceOrderResponse =
        api.post("/checkout/orders", request)

    /** The provider's hosted payment page, to open in a browser tab. */
    suspend fun startPayment(orderId: String): StartPaymentResponse =
        api.post("/checkout/orders/$orderId/pay")

    /**
     * Has the charge cleared?
     *
     * Polled after the customer returns from the payment page. Settlement is
     * confirmed by the provider's webhook with a nightly reconcile behind it, so
     * an order becomes paid whether or not the app ever asks — this only decides
     * how quickly the screen notices.
     */
    suspend fun paymentStatus(orderId: String): OrderStatusResponse =
        api.get("/checkout/orders/$orderId/status")
}
