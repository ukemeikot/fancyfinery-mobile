package com.fancyfinery.mobile.features.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.core.network.ApiErrorCode
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.features.catalog.data.CatalogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the catalogue screen.
 *
 * Three behaviours here are deliberate rather than incidental:
 *
 * **Search is debounced.** Every keystroke firing a request would put a dozen
 * calls on a mobile connection to answer one question, and the answers can
 * arrive out of order. [searchJob] is cancelled and restarted per keystroke, so
 * only the last one survives the pause.
 *
 * **A filter change does not blank the screen.** The previous results stay
 * visible under [CatalogState.isRefiltering] until the new page lands.
 *
 * **Paging appends, and ignores its own failures.** A failed second page must
 * not throw away a first page the customer is reading.
 */
class CatalogViewModel(
    private val repository: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        load()
    }

    /** First load: categories and the opening page, together. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // The catalogue is the screen; a categories outage must not take it
            // down, so that failure is tolerated and the filter row is simply
            // absent.
            val categories = runCatching { repository.categories().items }
                .getOrDefault(emptyList())

            runCatching { repository.products(limit = PAGE_SIZE) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = page.items,
                            categories = categories,
                            hasMore = page.hasMore,
                            error = null,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.withError(e) } }
        }
    }

    fun onCategorySelected(slug: String?) {
        if (_state.value.selectedCategory == slug) return
        _state.update { it.copy(selectedCategory = slug) }
        refilter()
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Long enough to cover ordinary typing, short enough not to feel
            // like a delay once the customer stops.
            delay(SEARCH_DEBOUNCE_MS)
            refilter()
        }
    }

    /** Re-run the query, keeping the current results on screen meanwhile. */
    private fun refilter() {
        viewModelScope.launch {
            val current = _state.value
            _state.update { it.copy(isRefiltering = true, error = null) }

            runCatching {
                repository.products(
                    categorySlug = current.selectedCategory,
                    query = current.query.takeIf { q -> q.isNotBlank() },
                    limit = PAGE_SIZE,
                )
            }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isRefiltering = false,
                            isLoading = false,
                            products = page.items,
                            hasMore = page.hasMore,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isRefiltering = false).withError(e) }
                }
        }
    }

    /**
     * Append the next page.
     *
     * Guarded against re-entry: a fast scroll fires this repeatedly, and without
     * the guard the same offset would be requested several times and appended
     * several times.
     */
    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore || current.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }

            runCatching {
                repository.products(
                    categorySlug = current.selectedCategory,
                    query = current.query.takeIf { q -> q.isNotBlank() },
                    limit = PAGE_SIZE,
                    offset = current.products.size,
                )
            }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            products = it.products + page.items,
                            hasMore = page.hasMore,
                        )
                    }
                }
                .onFailure {
                    // Keep what is already on screen. The customer is reading
                    // it, and replacing it with an error over a failed NEXT
                    // page would be the app losing their place.
                    _state.update { it.copy(isLoadingMore = false, hasMore = false) }
                }
        }
    }

    fun retry() = load()

    /**
     * Pull-to-refresh.
     *
     * Distinct from [load] in one way that matters: it does NOT set
     * `isLoading`, so the grid is never replaced by a skeleton while the
     * customer is holding the gesture. They are looking at the list; swapping
     * it for placeholders would be the app taking their content away as a
     * reward for asking for fresher content.
     *
     * The current category and search term are preserved — refreshing is "get
     * me this again, now", not "start over".
     */
    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val current = _state.value

            val categories = runCatching { repository.categories().items }.getOrNull()

            runCatching {
                repository.products(
                    categorySlug = current.selectedCategory,
                    query = current.query.takeIf { q -> q.isNotBlank() },
                    limit = PAGE_SIZE,
                )
            }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            isLoading = false,
                            products = page.items,
                            categories = categories ?: it.categories,
                            hasMore = page.hasMore,
                            error = null,
                        )
                    }
                }
                .onFailure { e ->
                    // Keep showing what is already there. A failed refresh means
                    // the list is stale, not gone.
                    _state.update {
                        if (it.products.isEmpty()) {
                            it.copy(isRefreshing = false).withError(e)
                        } else {
                            it.copy(isRefreshing = false)
                        }
                    }
                }
        }
    }

    private fun CatalogState.withError(e: Throwable): CatalogState {
        val api = e as? ApiException
        return copy(
            isLoading = false,
            error = api?.message ?: "Something went wrong. Please try again.",
            // Offer a retry only where one could plausibly help. A 422 will
            // fail again identically, and a button that never works is worse
            // than no button.
            retryable = api == null || api.code in RETRYABLE,
        )
    }

    companion object {
        private const val PAGE_SIZE = 24
        private const val SEARCH_DEBOUNCE_MS = 350L
        private val RETRYABLE = setOf(
            ApiErrorCode.Offline,
            ApiErrorCode.ServerError,
            ApiErrorCode.RateLimited,
            ApiErrorCode.Unknown,
        )
    }
}
