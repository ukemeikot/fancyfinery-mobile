package com.fancyfinery.mobile.features.auth.data.remote

import com.fancyfinery.mobile.core.network.ApiErrorCode
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.core.network.NetworkConfig
import com.fancyfinery.mobile.features.auth.data.remote.dto.AuthResponse
import com.fancyfinery.mobile.features.auth.data.remote.dto.BetterAuthError
import com.fancyfinery.mobile.features.auth.data.remote.dto.ForgotPasswordRequest
import com.fancyfinery.mobile.features.auth.data.remote.dto.GoogleIdTokenPayload
import com.fancyfinery.mobile.features.auth.data.remote.dto.GoogleIdTokenSignInRequest
import com.fancyfinery.mobile.features.auth.data.remote.dto.MagicLinkRequest
import com.fancyfinery.mobile.features.auth.data.remote.dto.ResetPasswordRequest
import com.fancyfinery.mobile.features.auth.data.remote.dto.SessionResponse
import com.fancyfinery.mobile.features.auth.data.remote.dto.SignInRequest
import com.fancyfinery.mobile.features.auth.data.remote.dto.SignUpRequest
import com.fancyfinery.mobile.features.auth.data.remote.dto.SocialSignInRequest
import com.fancyfinery.mobile.features.auth.data.remote.dto.SocialSignInResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Better Auth's REST surface, called directly.
 *
 * This deliberately does NOT go through [com.fancyfinery.mobile.core.network.ApiClient].
 * Better Auth predates our `/api/mobile/v1` endpoints and answers in its own
 * shape — `{"message": "...", "code": "INVALID_EMAIL_OR_PASSWORD"}` on failure,
 * and the session object itself on success, with no `data` wrapper. Forcing it
 * through the envelope decoder would fail on every single call.
 *
 * **How the app gets its token.** The `bearer` plugin is enabled server-side, so
 * a successful sign-in returns the session token in a `set-auth-token` RESPONSE
 * HEADER as well as the usual cookie. That header is the whole mechanism: it is
 * read here, stored, and replayed as `Authorization: Bearer …` on every later
 * request. Nothing parses or interprets the token — it names a row in
 * `auth_session`, which is what makes signing out genuinely revoke it.
 */
class AuthApi(
    private val http: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {

    /** Sign in with email and password. Returns the user and the bearer token. */
    suspend fun signIn(email: String, password: String): AuthResult =
        authenticating("/sign-in/email", SignInRequest(email = email, password = password))

    /**
     * Create an account.
     *
     * The server requires a verified address before sign-in succeeds
     * (`requireEmailVerification`), so this often returns no token even when it
     * succeeds — the customer has to open the email first. [AuthResult.token]
     * being null is therefore a normal outcome here, not a failure.
     */
    suspend fun signUp(name: String, email: String, password: String): AuthResult =
        authenticating(
            "/sign-up/email",
            SignUpRequest(name = name, email = email, password = password),
        )

    /** Send a one-time sign-in link to an address. */
    suspend fun sendMagicLink(email: String, callbackUrl: String) {
        call("/sign-in/magic-link", MagicLinkRequest(email = email, callbackURL = callbackUrl))
    }

    /**
     * Send a password-reset link.
     *
     * The path is `/request-password-reset`. Better Auth's older
     * `/forget-password` alias no longer exists in this version and answers
     * 404 — which surfaced as the button appearing to do nothing at all.
     * Endpoint paths here are part of a dependency's public API, not ours, so
     * they are worth checking against a running server after an upgrade rather
     * than assuming.
     */
    suspend fun forgotPassword(email: String, redirectTo: String) {
        call(
            "/request-password-reset",
            ForgotPasswordRequest(email = email, redirectTo = redirectTo),
        )
    }

    /**
     * Complete a password reset using the token from the emailed link.
     *
     * The token IS the credential — Better Auth minted it, emailed it, and
     * verifies it here. No session exists at this point and none is expected;
     * the customer has, by definition, forgotten how to make one.
     */
    suspend fun resetPassword(token: String, newPassword: String) {
        call("/reset-password", ResetPasswordRequest(token = token, newPassword = newPassword))
    }

    /** Re-send the address-confirmation email. */
    suspend fun resendVerification(email: String, callbackUrl: String) {
        call(
            "/send-verification-email",
            MagicLinkRequest(email = email, callbackURL = callbackUrl),
        )
    }

    /**
     * Exchange a Google ID token for a session.
     *
     * The native path. Better Auth verifies the token's signature against
     * Google and issues a session for the matching account — the same account
     * the website's Google button produces, because both name the same OAuth
     * client. The bearer token comes back in `set-auth-token` exactly as it
     * does for an email sign-in.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult =
        authenticating(
            "/sign-in/social",
            GoogleIdTokenSignInRequest(idToken = GoogleIdTokenPayload(token = idToken)),
        )

    /**
     * Begin a browser-based Google sign-in and return the URL to open.
     *
     * Nothing is signed in yet at this point: the response is only where to send
     * the customer. The session is established when Google redirects back to the
     * server's callback.
     */
    suspend fun googleAuthUrl(callbackUrl: String): String {
        val response = send {
            http.post(auth("/sign-in/social")) {
                setBody(
                    SocialSignInRequest(provider = "google", callbackURL = callbackUrl),
                )
            }
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw translate(response, body)
        return json.decodeFromString<SocialSignInResponse>(body).url
    }

    /** The current session, or null when the stored token is no longer valid. */
    suspend fun session(): SessionResponse? {
        val response = send { http.get(auth("/get-session")) }
        if (!response.status.isSuccess()) return null
        val body = response.bodyAsText()
        // Better Auth answers a literal `null` with a 200 when there is no
        // session, which is not an error and must not be decoded as one.
        if (body.isBlank() || body.trim() == "null") return null
        return runCatching { json.decodeFromString<SessionResponse>(body) }.getOrNull()
    }

    /**
     * Revoke the session server-side.
     *
     * Best-effort by design: the caller clears the local token regardless. A
     * customer who taps "sign out" on a dead connection must still end up signed
     * out on their own device.
     */
    suspend fun signOut() {
        runCatching { send { http.post(auth("/sign-out")) } }
    }

    // --- internals ----------------------------------------------------------

    /** A call that is expected to establish a session. */
    private suspend fun authenticating(path: String, body: Any): AuthResult {
        val response = send {
            http.post(auth(path)) { setBody(body) }
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw translate(response, text)

        val parsed = runCatching { json.decodeFromString<AuthResponse>(text) }.getOrNull()
        return AuthResult(
            token = response.headers[SET_AUTH_TOKEN] ?: parsed?.token,
            user = parsed?.user,
        )
    }

    /** A call whose only outcome that matters is success or failure. */
    private suspend fun call(path: String, body: Any) {
        val response = send { http.post(auth(path)) { setBody(body) } }
        if (!response.status.isSuccess()) {
            throw translate(response, response.bodyAsText())
        }
    }

    private suspend inline fun send(request: () -> HttpResponse): HttpResponse =
        try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ApiException(
                code = ApiErrorCode.Offline,
                message = "Can't reach Fancy Finery. Check your connection and try again.",
            )
        }

    /**
     * Turn a Better Auth failure into an [ApiException].
     *
     * Its `message` is already written for a person to read, so it is passed
     * through rather than replaced — "Invalid email or password" is exactly what
     * the screen should say, and inventing our own wording would only risk
     * saying something less true.
     */
    private fun translate(response: HttpResponse, body: String): ApiException {
        val error = runCatching { json.decodeFromString<BetterAuthError>(body) }.getOrNull()
        val code = when (response.status.value) {
            401, 403 -> ApiErrorCode.Unauthenticated
            404 -> ApiErrorCode.NotFound
            409 -> ApiErrorCode.Conflict
            422, 400 -> ApiErrorCode.InvalidRequest
            429 -> ApiErrorCode.RateLimited
            in 500..599 -> ApiErrorCode.ServerError
            else -> ApiErrorCode.Unknown
        }
        return ApiException(
            code = code,
            message = error?.message ?: "Could not sign you in. Please try again.",
        )
    }

    private fun auth(path: String) = NetworkConfig.AUTH_PREFIX + path

    private companion object {
        /** Header the bearer plugin returns a freshly-minted session token in. */
        const val SET_AUTH_TOKEN = "set-auth-token"
    }
}

/**
 * The outcome of an authenticating call.
 *
 * [token] is null when the account was created but cannot sign in yet, which is
 * the normal path for a new email/password account: the address has to be
 * confirmed first. The UI uses that to decide between "you're in" and "check
 * your email".
 */
data class AuthResult(
    val token: String?,
    val user: com.fancyfinery.mobile.features.auth.data.remote.dto.UserDto?,
) {
    val isSignedIn: Boolean get() = token != null
}
