package com.fancyfinery.mobile.features.wishlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.core.network.ApiErrorCode
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.core.session.SessionStore
import com.fancyfinery.mobile.features.wishlist.data.WishlistItemDto
import com.fancyfinery.mobile.features.wishlist.data.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WishlistState(
    val isLoading: Boolean = true,
    val items: List<WishlistItemDto> = emptyList(),
    /** Distinct from an empty list: the customer is signed out. */
    val needsAuth: Boolean = false,
    val error: String? = null,
)

class WishlistViewModel(
    private val repository: WishlistRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(WishlistState())
    val state: StateFlow<WishlistState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (!session.isSignedIn) {
            _state.update { it.copy(isLoading = false, needsAuth = true) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, needsAuth = false, error = null) }
            runCatching { repository.list() }
                .onSuccess { response ->
                    _state.update {
                        it.copy(isLoading = false, items = response.items)
                    }
                }
                .onFailure { e ->
                    val unauthenticated =
                        (e as? ApiException)?.code == ApiErrorCode.Unauthenticated
                    _state.update {
                        it.copy(
                            isLoading = false,
                            needsAuth = unauthenticated,
                            error = if (unauthenticated) null else (e as? ApiException)?.message,
                        )
                    }
                }
        }
    }

    /**
     * Remove, optimistically.
     *
     * The row disappears immediately and is restored only if the server
     * refuses. Waiting for a round trip before a heart stops being a heart is
     * the kind of latency people read as the app being broken.
     */
    fun onRemove(productId: String) {
        val previous = _state.value.items
        _state.update { it.copy(items = it.items.filterNot { item -> item.productId == productId }) }

        viewModelScope.launch {
            runCatching { repository.remove(productId) }
                .onFailure { _state.update { it.copy(items = previous) } }
        }
    }
}
