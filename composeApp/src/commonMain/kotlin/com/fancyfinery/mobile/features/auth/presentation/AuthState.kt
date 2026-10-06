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
    val generalError: String? = null,
)
