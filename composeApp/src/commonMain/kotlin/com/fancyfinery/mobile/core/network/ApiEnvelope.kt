package com.fancyfinery.mobile.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire format every `/api/mobile/v1` endpoint answers in.
 *
 * Success is `{ "data": ... }` and failure is `{ "error": { code, message } }`,
 * for every endpoint without exception. That uniformity is why this file is
 * three small classes instead of one wrapper per call: the server fixed the
 * envelope precisely so the client would need exactly one.
 */
@Serializable
data class ApiEnvelope<T>(
    val data: T? = null,
    val error: ApiErrorBody? = null,
)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
    /** Names the offending input for a validation failure, e.g. "email". */
    val field: String? = null,
)

/**
 * A failed call, as the rest of the app sees it.
 *
 * Deliberately an exception rather than a sealed result type: almost every call
 * site wants the happy path and one catch, and a `Result`-shaped API would have
 * every repository function unwrapping and rewrapping the same two cases.
 *
 * [code] is matched on; [message] is shown to the customer. The server treats
 * codes as a contract and messages as free text, and so does this.
 */
class ApiException(
    val code: ApiErrorCode,
    override val message: String,
    val field: String? = null,
) : Exception(message)

/**
 * The failure kinds the UI branches on.
 *
 * [Unknown] exists so that a code this build has never heard of still arrives
 * with its server-written message intact, rather than being flattened into
 * "something went wrong". A newer server can add a code without breaking an
 * older app.
 */
enum class ApiErrorCode {
    @SerialName("unauthenticated")
    Unauthenticated,

    @SerialName("forbidden")
    Forbidden,

    @SerialName("not_found")
    NotFound,

    @SerialName("invalid_request")
    InvalidRequest,

    @SerialName("out_of_stock")
    OutOfStock,

    @SerialName("payment_unavailable")
    PaymentUnavailable,

    @SerialName("conflict")
    Conflict,

    @SerialName("rate_limited")
    RateLimited,

    @SerialName("server_error")
    ServerError,

    /** No network, a DNS failure, a timeout — the request never got an answer. */
    Offline,

    Unknown;

    companion object {
        fun from(raw: String): ApiErrorCode = when (raw) {
            "unauthenticated" -> Unauthenticated
            "forbidden" -> Forbidden
            "not_found" -> NotFound
            "invalid_request" -> InvalidRequest
            "out_of_stock" -> OutOfStock
            "payment_unavailable" -> PaymentUnavailable
            "conflict" -> Conflict
            "rate_limited" -> RateLimited
            "server_error" -> ServerError
            else -> Unknown
        }
    }
}
