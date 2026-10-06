package com.fancyfinery.mobile.features.catalog.presentation

import com.fancyfinery.mobile.features.catalog.data.remote.dto.CategoryDto
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductSummaryDto

/**
 * What the catalogue screen is showing.
 *
 * [isLoading] is the FIRST load only. Paging and re-filtering set
 * [isLoadingMore] and [isRefiltering] instead, because swapping the whole
 * screen for a spinner every time someone taps a category makes the app feel
 * like it is starting over — the previous results should stay on screen,
 * dimmed, until the new ones arrive.
 */
data class CatalogState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefiltering: Boolean = false,
    /** Pull-to-refresh in flight. Never blanks the list — see `refresh()`. */
    val isRefreshing: Boolean = false,
    val products: List<ProductSummaryDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    /** Null = "All". Otherwise a category slug. */
    val selectedCategory: String? = null,
    val query: String = "",
    val hasMore: Boolean = false,
    /** Set when the load failed outright and there is nothing to show. */
    val error: String? = null,
    /** True when the failure is worth offering a retry for (offline, 5xx). */
    val retryable: Boolean = false,
) {
    /**
     * An empty result that is NOT a failure — a search or filter matched
     * nothing. Distinct from [error] because the screen should say "nothing
     * matched", not "something went wrong", and must not offer a retry button
     * for a query that will keep matching nothing.
     */
    val isEmpty: Boolean
        get() = !isLoading && error == null && products.isEmpty()
}
