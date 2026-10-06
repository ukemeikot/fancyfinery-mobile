package com.fancyfinery.mobile.features.account.data

import com.fancyfinery.mobile.core.network.ApiClient
import kotlinx.serialization.Serializable

@Serializable
private data class ProfilePatch(
    val fullName: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
private data class CancelResponse(val cancelled: Boolean = false)

/**
 * The customer's own data: profile, address book and order history.
 *
 * Nothing here takes a user id. Every endpoint scopes itself to the session the
 * bearer token resolves to, so there is no parameter through which one customer
 * could ask for another's orders — the absence is the control.
 */
class AccountRepository(private val api: ApiClient) {

    suspend fun profile(): ProfileDto = api.get("/account/profile")

    suspend fun updateProfile(
        fullName: String?,
        phone: String?,
        address: String?,
        city: String?,
        state: String?,
        country: String?,
    ): ProfileDto = api.patch(
        "/account/profile",
        ProfilePatch(
            fullName = fullName,
            phone = phone,
            address = address,
            city = city,
            state = state,
            country = country,
        ),
    )

    suspend fun orders(): OrderListResponse = api.get("/account/orders")

    suspend fun order(id: String): OrderDetailResponse = api.get("/account/orders/$id")

    /**
     * Cancel an unpaid order.
     *
     * The server decides in a single conditional statement — right owner, right
     * order, not paid, not already progressed — so a payment landing mid-request
     * cannot result in a cancelled order the customer has just been charged for.
     * A refusal arrives as a conflict, which the caller surfaces verbatim.
     */
    suspend fun cancelOrder(id: String): Boolean =
        api.delete<CancelResponse>("/account/orders/$id").cancelled
}
