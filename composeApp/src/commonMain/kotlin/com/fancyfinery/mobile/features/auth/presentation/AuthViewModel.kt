package com.fancyfinery.mobile.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.features.auth.AuthMode
import com.fancyfinery.mobile.features.auth.OnAuthSuccess
import com.fancyfinery.mobile.features.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the sign-in / create-account screen.
 *
 * Validation here mirrors the server's rules rather than inventing stricter
 * ones — an eight-character minimum because that is `minPasswordLength` in the
 * Better Auth config. Client-side checks exist to save a round trip, not to be
 * the control: the server re-validates everything.
 */
class AuthViewModel(
    private val repository: AuthRepository,
    initialMode: AuthMode = AuthMode.LOGIN,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState(mode = initialMode))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun onToggleMode() {
        _state.update { current ->
            // Deliberately resets the form rather than carrying fields across.
            // The two modes ask for different things, and a half-filled
            // register form showing under "Sign in" is confusing.
            AuthState(
                mode = when (current.mode) {
                    AuthMode.LOGIN -> AuthMode.REGISTER
                    AuthMode.REGISTER -> AuthMode.LOGIN
                },
                email = current.email,
            )
        }
    }

    fun onNameChange(value: String) =
        _state.update { it.copy(name = value, nameError = null, generalError = null) }

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, generalError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, generalError = null) }

    fun onConfirmPasswordChange(value: String) =
        _state.update { it.copy(confirmPassword = value, confirmPasswordError = null) }

    fun onSubmit(onSuccess: OnAuthSuccess) {
        if (!validate()) return
        val current = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null, notice = null) }

            runCatching {
                when (current.mode) {
                    AuthMode.LOGIN -> repository.signIn(
                        email = current.email,
                        password = current.password,
                    )

                    AuthMode.REGISTER -> repository.signUp(
                        name = current.name,
                        email = current.email,
                        password = current.password,
                    )
                }
            }
                .onSuccess { user ->
                    if (user != null) {
                        onSuccess()
                    } else {
                        // Authenticated but no session: the address is not
                        // confirmed yet. Say so plainly — this is the single
                        // most common point of confusion in a verify-first
                        // signup, and "nothing happened" is the worst answer.
                        _state.update {
                            it.copy(
                                isLoading = false,
                                notice = "Almost there — open the link we emailed to " +
                                    "${current.email.trim()} to confirm your account.",
                            )
                        }
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false).withError(e) } }
        }
    }

    /** Email a one-time sign-in link instead of using a password. */
    fun onMagicLink() {
        val email = _state.value.email.trim()
        if (!email.contains('@')) {
            _state.update { it.copy(emailError = "Enter your email address first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null, notice = null) }
            runCatching { repository.sendMagicLink(email) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            notice = "We've sent a sign-in link to $email.",
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false).withError(e) } }
        }
    }

    fun onForgotPassword() {
        val email = _state.value.email.trim()
        if (!email.contains('@')) {
            _state.update { it.copy(emailError = "Enter your email address first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null, notice = null) }
            runCatching { repository.forgotPassword(email) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            // Phrased so it is true whether or not an account
                            // exists: confirming which addresses are registered
                            // would turn this into an account-enumeration oracle.
                            notice = "If there's an account for $email, " +
                                "a reset link is on its way.",
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false).withError(e) } }
        }
    }

    /**
     * Start Google sign-in.
     *
     * Returns the URL to open in a browser tab through [onOpenUrl]; the session
     * is established when Google redirects back to the server. Nothing is
     * signed in at this point.
     */
    fun onGoogleSignIn(callbackUrl: String, onOpenUrl: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isGoogleLoading = true, generalError = null) }
            runCatching { repository.googleAuthUrl(callbackUrl) }
                .onSuccess { url ->
                    _state.update { it.copy(isGoogleLoading = false) }
                    onOpenUrl(url)
                }
                .onFailure { e ->
                    _state.update { it.copy(isGoogleLoading = false).withError(e) }
                }
        }
    }

    /** Adopt a token handed back by the Google redirect. */
    fun onGoogleToken(token: String, onSuccess: OnAuthSuccess) {
        viewModelScope.launch {
            _state.update { it.copy(isGoogleLoading = true) }
            runCatching { repository.adoptToken(token) }
                .onSuccess { user ->
                    _state.update { it.copy(isGoogleLoading = false) }
                    if (user != null) {
                        onSuccess()
                    } else {
                        _state.update {
                            it.copy(generalError = "That sign-in didn't complete. Please try again.")
                        }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isGoogleLoading = false).withError(e) }
                }
        }
    }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    private fun AuthState.withError(e: Throwable): AuthState = copy(
        generalError = (e as? ApiException)?.message
            ?: "Something went wrong. Please try again.",
    )

    private fun validate(): Boolean {
        val s = _state.value
        val nameError =
            if (s.mode == AuthMode.REGISTER && s.name.isBlank()) "Name is required" else null
        val emailError = when {
            s.email.isBlank() -> "Email is required"
            !s.email.contains('@') -> "Enter a valid email address"
            else -> null
        }
        val passwordError = when {
            s.password.isBlank() -> "Password is required"
            // Matches minPasswordLength in the Better Auth config.
            s.password.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }
        val confirmError =
            if (s.mode == AuthMode.REGISTER && s.confirmPassword != s.password) {
                "Passwords do not match"
            } else null

        _state.update {
            it.copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
            )
        }
        return nameError == null && emailError == null &&
            passwordError == null && confirmError == null
    }
}
