package com.fancyfinery.mobile.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException

/**
 * One place that knows how to talk to `/api/mobile/v1`.
 *
 * Every repository goes through here, which keeps three rules in one place
 * rather than scattered across a dozen call sites:
 *
 *  1. **Unwrap the envelope.** Success is `{"data": ...}`; callers get the
 *     payload, never the wrapper.
 *  2. **Turn every failure into [ApiException].** Including failures that are
 *     not JSON at all — a proxy's HTML 502, a captive portal's redirect, a
 *     dropped connection — so no repository has to tell a network failure from
 *     a rejected request.
 *  3. **Never swallow cancellation.** A blanket `catch (e: Exception)` around a
 *     suspending call will happily eat the [CancellationException] that
 *     structured concurrency depends on, leaving a scope that cannot be
 *     cancelled and a screen that keeps loading after the user has left it. It
 *     is rethrown first, before anything else is considered.
 *
 * Non-2xx responses are NOT treated as transport failures: the server answers
 * its error envelope with a real status, so the body is parsed either way and
 * the `error` field is what decides the outcome.
 */
class ApiClient(@PublishedApi internal val http: HttpClient) {

    suspend inline fun <reified T> get(
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {},
    ): T = unwrap(send { http.get(url(path)) { block() } })

    suspend inline fun <reified T> post(
        path: String,
        body: Any? = null,
        crossinline block: HttpRequestBuilder.() -> Unit = {},
    ): T = unwrap(
        send {
            http.post(url(path)) {
                if (body != null) setBody(body)
                block()
            }
        },
    )

    suspend inline fun <reified T> patch(path: String, body: Any? = null): T =
        unwrap(send { http.patch(url(path)) { if (body != null) setBody(body) } })

    suspend inline fun <reified T> delete(
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {},
    ): T = unwrap(send { http.delete(url(path)) { block() } })

    /**
     * Perform the call, converting a transport failure into [ApiErrorCode.Offline].
     *
     * Separate from parsing so that "could not reach the server" and "the server
     * said no" stay distinguishable — they need different words in the UI and
     * only one of them is worth a retry button.
     */
    @PublishedApi
    internal suspend inline fun send(call: () -> HttpResponse): HttpResponse =
        try {
            call()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ApiException(
                code = ApiErrorCode.Offline,
                message = "Can't reach Fancy Finery. Check your connection and try again.",
            )
        }

    @PublishedApi
    internal suspend inline fun <reified T> unwrap(response: HttpResponse): T {
        val envelope: ApiEnvelope<T> = try {
            response.body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Not our envelope: a proxy error page, a captive portal, a
            // truncated body. The status code is the only honest signal left.
            throw ApiException(
                code = statusToCode(response.status.value),
                message = "Something went wrong. Please try again.",
            )
        }

        envelope.error?.let { error ->
            throw ApiException(
                code = ApiErrorCode.from(error.code),
                message = error.message,
                field = error.field,
            )
        }

        return envelope.data ?: throw ApiException(
            code = ApiErrorCode.ServerError,
            message = "The server returned an empty response.",
        )
    }

    @PublishedApi
    internal fun url(path: String): String =
        if (path.startsWith("http")) path else NetworkConfig.API_PREFIX + path

    @PublishedApi
    internal fun statusToCode(status: Int): ApiErrorCode = when (status) {
        401 -> ApiErrorCode.Unauthenticated
        403 -> ApiErrorCode.Forbidden
        404 -> ApiErrorCode.NotFound
        409 -> ApiErrorCode.Conflict
        422 -> ApiErrorCode.InvalidRequest
        429 -> ApiErrorCode.RateLimited
        in 500..599 -> ApiErrorCode.ServerError
        else -> ApiErrorCode.Unknown
    }
}
