package com.fancyfinery.mobile.features.auth.presentation

import com.fancyfinery.mobile.features.auth.AuthMode

data class AuthState(
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    /** Google sign-in in flight — a separate flag so only that button spins. */
    val isGoogleLoading: Boolean = false,
    val generalError: String? = null,
    /**
     * A success that is NOT a sign-in: "we've emailed you".
     *
     * Needed because three different actions end this way — creating an account
     * that must be confirmed, requesting a magic link, and requesting a password
     * reset — and in all three the customer's next step is their inbox, not this
     * screen. Treating them as failures (nothing happened) or as sign-ins
     * (letting them through) would both be wrong.
     */
    val notice: String? = null,
)
