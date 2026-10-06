package com.fancyfinery.mobile.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.features.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResetPasswordState(
    val password: String = "",
    val confirm: String = "",
    val passwordError: String? = null,
    val confirmError: String? = null,
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
    val error: String? = null,
)

class ResetPasswordViewModel(
    private val token: String,
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ResetPasswordState())
    val state: StateFlow<ResetPasswordState> = _state.asStateFlow()

    fun onPassword(value: String) =
        _state.update { it.copy(password = value, passwordError = null, error = null) }

    fun onConfirm(value: String) =
        _state.update { it.copy(confirm = value, confirmError = null) }

    fun onSubmit() {
        val current = _state.value

        // Matches minPasswordLength in the Better Auth config. Checked here to
        // save a round trip, not as the control — the server re-validates.
        val passwordError = when {
            current.password.length < MIN_LENGTH ->
                "Use at least $MIN_LENGTH characters"
            else -> null
        }
        val confirmError =
            if (current.confirm != current.password) "The two passwords don't match" else null

        if (passwordError != null || confirmError != null) {
            _state.update { it.copy(passwordError = passwordError, confirmError = confirmError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            runCatching { repository.resetPassword(token = token, newPassword = current.password) }
                .onSuccess { _state.update { it.copy(isSaving = false, isDone = true) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            // An expired or already-used token is the common
                            // failure, and the server says so. Passing its
                            // wording through tells the customer to request a
                            // new link rather than retyping the same password.
                            error = (e as? ApiException)?.message
                                ?: "Could not update your password. The link may have expired.",
                        )
                    }
                }
        }
    }

    private companion object {
        const val MIN_LENGTH = 8
    }
}
