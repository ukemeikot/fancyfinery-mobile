package com.fancyfinery.mobile.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.features.catalog.data.local.RecentlyViewedDao
import com.fancyfinery.mobile.features.catalog.data.local.RecentlyViewedEntity
import com.fancyfinery.mobile.features.catalog.data.remote.dto.CategoryDto
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductSummaryDto
import com.fancyfinery.mobile.features.home.data.HomeRepository
import com.fancyfinery.mobile.features.home.data.HomeReviewDto
import com.fancyfinery.mobile.features.home.data.LookbookEntryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val featured: List<ProductSummaryDto> = emptyList(),
    val newArrivals: List<ProductSummaryDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val lookbook: List<LookbookEntryDto> = emptyList(),
    val reviews: List<HomeReviewDto> = emptyList(),
    val recentlyViewed: List<RecentlyViewedEntity> = emptyList(),
    val isJoining: Boolean = false,
    val newsletterMessage: String? = null,
    val error: String? = null,
) {
    val isEmpty: Boolean
        get() = featured.isEmpty() && newArrivals.isEmpty() && categories.isEmpty()
}

class HomeViewModel(
    private val repository: HomeRepository,
    private val recentlyViewed: RecentlyViewedDao,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        load()
        // Local, so it updates the moment a product is opened rather than only
        // on the next fetch.
        viewModelScope.launch {
            recentlyViewed.observeRecent().collect { entries ->
                _state.update { it.copy(recentlyViewed = entries) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            fetch()
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            fetch()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun fetch() {
        runCatching { repository.home() }
            .onSuccess { home ->
                _state.update {
                    it.copy(
                        featured = home.featured,
                        newArrivals = home.newArrivals,
                        categories = home.categories,
                        lookbook = home.lookbook,
                        reviews = home.reviews,
                        error = null,
                    )
                }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(
                        error = (e as? ApiException)?.message
                            ?: "Could not load the shop.",
                    )
                }
            }
    }

    fun onJoinPriveCircle(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(isJoining = true, newsletterMessage = null) }
            runCatching { repository.joinPriveCircle(email = email, name = null) }
                .onSuccess { response ->
                    _state.update {
                        it.copy(isJoining = false, newsletterMessage = response.message)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isJoining = false,
                            newsletterMessage = (e as? ApiException)?.message
                                ?: "Could not add you to the list.",
                        )
                    }
                }
        }
    }
}
