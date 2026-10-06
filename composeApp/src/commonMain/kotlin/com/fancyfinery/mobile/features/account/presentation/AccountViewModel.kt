package com.fancyfinery.mobile.features.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.core.network.ApiErrorCode
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.core.session.SessionStore
import com.fancyfinery.mobile.features.account.data.AccountRepository
import com.fancyfinery.mobile.features.account.data.OrderSummaryDto
import com.fancyfinery.mobile.features.account.data.ProfileDto
import com.fancyfinery.mobile.features.auth.data.AuthRepository
import com.fancyfinery.mobile.features.wishlist.data.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountState(
    val isLoading: Boolean = true,
    val needsAuth: Boolean = false,
    val profile: ProfileDto? = null,
    val orders: List<OrderSummaryDto> = emptyList(),
    val currency: String = "NGN",
    val isSaving: Boolean = false,
    val notice: String? = null,
    val error: String? = null,
)

/**
 * The account hub: who you are, what you ordered, and the way out.
 *
 * Profile and orders load together because the screen shows both, and two
 * sequential round trips on a mobile connection is twice the wait for one
 * screen. An orders failure does not block the profile — a customer should
 * still be able to edit their address when order history is briefly unavailable.
 */
class AccountViewModel(
    private val account: AccountRepository,
    private val auth: AuthRepository,
    private val wishlist: WishlistRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountState(currency = session.currency.value))
    val state: StateFlow<AccountState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (!session.isSignedIn) {
            _state.update { it.copy(isLoading = false, needsAuth = true) }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, needsAuth = false, error = null)
            }

            val profile = runCatching { account.profile() }
            if (profile.isFailure) {
                val e = profile.exceptionOrNull()
                val unauthenticated =
                    (e as? ApiException)?.code == ApiErrorCode.Unauthenticated
                if (unauthenticated) {
                    // The stored token is no longer honoured. Clear it rather
                    // than leaving a signed-in shell that fails every request.
                    session.clear()
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        needsAuth = unauthenticated,
                        error = if (unauthenticated) null else (e as? ApiException)?.message,
                    )
                }
                return@launch
            }

            val orders = runCatching { account.orders().items }.getOrDefault(emptyList())

            _state.update {
                it.copy(
                    isLoading = false,
                    profile = profile.getOrNull(),
                    orders = orders,
                    currency = session.currency.value,
                )
            }
        }
    }

    fun onSaveProfile(
        fullName: String,
        phone: String,
        address: String,
        city: String,
        state: String,
        country: String,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, notice = null, error = null) }
            runCatching {
                account.updateProfile(
                    fullName = fullName.ifBlank { null },
                    phone = phone.ifBlank { null },
                    address = address.ifBlank { null },
                    city = city.ifBlank { null },
                    state = state.ifBlank { null },
                    country = country.ifBlank { null },
                )
            }
                .onSuccess { updated ->
                    _state.update {
                        it.copy(isSaving = false, profile = updated, notice = "Saved.")
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = (e as? ApiException)?.message
                                ?: "Could not save your details.",
                        )
                    }
                }
        }
    }

    /** Change the currency everything is priced and charged in. */
    fun onCurrencyChange(code: String) {
        viewModelScope.launch {
            session.setCurrency(code)
            _state.update { it.copy(currency = code) }
            // Re-read with the new currency so order history and totals are
            // rendered consistently.
            load()
        }
    }

    fun onSignOut(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { auth.signOut() }
            wishlist.forget()
            _state.update { AccountState(isLoading = false, needsAuth = true) }
            onDone()
        }
    }

    fun dismissNotice() = _state.update { it.copy(notice = null, error = null) }
}
