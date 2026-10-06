package com.fancyfinery.mobile.features.account.data

import com.fancyfinery.mobile.features.catalog.data.remote.dto.MoneyDto
import kotlinx.serialization.Serializable

// Wire shapes for the account, checkout and shipping endpoints, mirroring
// the server's DTOs. (Written as line comments: Kotlin nests block comments, so
// a slash-star path inside KDoc opens one that is never closed.)

@Serializable
data class SavedAddressDto(
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
data class ProfileDto(
    val id: String,
    val email: String? = null,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    /** Decides whether a staff entry point is drawn. NOT a permission — every
     *  admin surface re-checks the role server-side. */
    val role: String = "customer",
    val address: SavedAddressDto? = null,
)

@Serializable
data class OrderItemDto(
    val id: String,
    val productId: String? = null,
    val variantId: String? = null,
    val name: String,
    val qty: Int,
    val unitPrice: MoneyDto,
    val lineTotal: MoneyDto,
)

@Serializable
data class OrderAddressDto(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val apartment: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val postal: String? = null,
)

@Serializable
data class OrderSummaryDto(
    val id: String,
    val status: String,
    val paymentStatus: String,
    val total: MoneyDto,
    val currency: String,
    val itemCount: Int = 0,
    val trackingNumber: String? = null,
    val createdAt: String,
    /** An online charge can still be started for this order. */
    val payable: Boolean = false,
    /** The customer may still cancel it themselves. */
    val cancellable: Boolean = false,
)

@Serializable
data class OrderDetailDto(
    val id: String,
    val status: String,
    val paymentStatus: String,
    val total: MoneyDto,
    val currency: String,
    val itemCount: Int = 0,
    val trackingNumber: String? = null,
    val createdAt: String,
    val payable: Boolean = false,
    val cancellable: Boolean = false,
    val items: List<OrderItemDto> = emptyList(),
    val subtotal: MoneyDto,
    val shipping: MoneyDto,
    val tax: MoneyDto,
    val discount: MoneyDto,
    val shippingMethod: String? = null,
    val courierName: String? = null,
    val estimatedMinDays: Int? = null,
    val estimatedMaxDays: Int? = null,
    val discountCode: String? = null,
    val taxLabel: String? = null,
    val paidAt: String? = null,
    val address: OrderAddressDto,
)

@Serializable
data class OrderListResponse(val items: List<OrderSummaryDto> = emptyList())

@Serializable
data class OrderDetailResponse(val order: OrderDetailDto)

@Serializable
data class OrderStatusResponse(
    val orderId: String,
    val paymentStatus: String,
    val status: String,
    val paidAt: String? = null,
)

// --- Checkout ---------------------------------------------------------------

@Serializable
data class CheckoutLineRequest(
    val productId: String,
    val variantId: String? = null,
    val qty: Int,
)

/** Mirrors the server's `checkoutSchema` exactly; a renamed field is dropped. */
@Serializable
data class PlaceOrderRequest(
    val name: String,
    val email: String,
    val phone: String,
    val countryCode: String,
    val country: String,
    val state: String,
    val city: String,
    val postal: String,
    val address: String,
    val apartment: String? = null,
    val courierId: String? = null,
    val ngDestinationId: String? = null,
    val couponCode: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val items: List<CheckoutLineRequest>,
)

@Serializable
data class PlaceOrderResponse(
    val orderId: String,
    /** True → call `/checkout/orders/{id}/pay` next. */
    val requiresPayment: Boolean = false,
    val paymentProvider: String? = null,
    val currency: String = "NGN",
)

@Serializable
data class StartPaymentResponse(val paymentUrl: String)

// --- Shipping ---------------------------------------------------------------

@Serializable
data class CountryDto(
    val code: String,
    val name: String,
    val zone: String? = null,
    val flag: String? = null,
)

@Serializable
data class CountryListResponse(val items: List<CountryDto> = emptyList())

@Serializable
data class NgStateDto(val id: String, val name: String, val code: String? = null)

@Serializable
data class NgStateListResponse(val items: List<NgStateDto> = emptyList())

@Serializable
data class NgAreaDto(
    val id: String,
    val stateId: String,
    val name: String,
    val fee: MoneyDto,
)

@Serializable
data class NgAreaListResponse(val items: List<NgAreaDto> = emptyList())

@Serializable
data class QuoteRequest(
    val countryCode: String,
    val items: List<CheckoutLineRequest>,
    val courierId: String? = null,
    val ngDestinationId: String? = null,
    val couponCode: String? = null,
)

@Serializable
data class QuoteOptionDto(
    val courierId: String,
    val courierCode: String? = null,
    val courierName: String,
    val price: MoneyDto,
    val free: Boolean = false,
    val minDays: Int? = null,
    val maxDays: Int? = null,
)

@Serializable
data class QuoteBreakdownDto(
    val subtotal: MoneyDto,
    val shipping: MoneyDto,
    val tax: MoneyDto,
    val discount: MoneyDto,
    val total: MoneyDto,
    val taxLabel: String = "",
    val taxRateBps: Int? = null,
    val discountCode: String? = null,
)

@Serializable
data class QuoteCouponDto(
    val applied: Boolean = false,
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class QuoteResponse(
    val countryCode: String,
    val currency: String,
    val weightGrams: Int = 0,
    val weightLabel: String = "",
    val zoneName: String? = null,
    val bracketLabel: String? = null,
    val options: List<QuoteOptionDto> = emptyList(),
    val selectedCourierId: String? = null,
    val breakdown: QuoteBreakdownDto,
    val coupon: QuoteCouponDto = QuoteCouponDto(),
    /** Null when quotable; otherwise why nothing is on offer. */
    val unavailable: String? = null,
)

// --- Config -----------------------------------------------------------------

@Serializable
data class CurrencyDto(
    val code: String,
    val symbol: String,
    val name: String,
    val flag: String? = null,
    /** False → orders in this currency are placed as pay-on-delivery. */
    val payable: Boolean = false,
)

@Serializable
data class AppConfigDto(
    val siteName: String = "Fancy Finery",
    val siteUrl: String = "",
    val currencies: List<CurrencyDto> = emptyList(),
    val minimumBuild: Int = 1,
)
