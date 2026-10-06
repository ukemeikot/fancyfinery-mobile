package com.fancyfinery.mobile.features.auth.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Better Auth's request and response shapes.
 *
 * These mirror the library's own API, not ours, which is why they are not in
 * the house envelope. Field names match what Better Auth expects exactly —
 * `callbackURL` with that capitalisation included, because it is sent as JSON
 * and a rename would silently drop it.
 */

// --- Requests ---------------------------------------------------------------

@Serializable
data class SignInRequest(
    val email: String,
    val password: String,
    /**
     * Ask the server for a long-lived session.
     *
     * Default on purpose. A phone is a personal device and being signed out of
     * a shop every few days is a reason people stop opening an app. The session
     * remains revocable server-side, so this trades no control for the
     * convenience.
     */
    val rememberMe: Boolean = true,
)

@Serializable
data class SignUpRequest(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class MagicLinkRequest(
    val email: String,
    val callbackURL: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
    val redirectTo: String,
)

@Serializable
data class SocialSignInRequest(
    val provider: String,
    val callbackURL: String,
)

// --- Responses --------------------------------------------------------------

@Serializable
data class UserDto(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    val image: String? = null,
    val emailVerified: Boolean = false,
)

@Serializable
data class AuthResponse(
    val user: UserDto? = null,
    /**
     * Some Better Auth builds echo the session token in the body as well as in
     * the `set-auth-token` header. Read as a fallback; the header is primary.
     */
    val token: String? = null,
    val redirect: Boolean = false,
    val url: String? = null,
)

@Serializable
data class SessionDto(
    val id: String,
    val userId: String,
    val expiresAt: String? = null,
)

@Serializable
data class SessionResponse(
    val user: UserDto? = null,
    val session: SessionDto? = null,
)

@Serializable
data class SocialSignInResponse(
    val url: String,
    val redirect: Boolean = true,
)

/** Better Auth's failure body. Its `message` is already customer-readable. */
@Serializable
data class BetterAuthError(
    val message: String? = null,
    val code: String? = null,
)
