package com.fancyfinery.mobile.features.catalog.data

import com.fancyfinery.mobile.core.network.ApiClient
import com.fancyfinery.mobile.features.catalog.data.remote.dto.CategoryListResponse
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductDetailResponse
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductListResponse
import io.ktor.client.request.parameter

/**
 * Reads of the published catalogue.
 *
 * Thin on purpose. Pricing, currency conversion, stock and which products are
 * visible at all are decided server-side — this type's whole job is to ask, not
 * to interpret. Anything that looks like a merchandising rule appearing in here
 * is a sign it has been duplicated out of the storefront.
 */
class CatalogRepository(private val api: ApiClient) {

    /**
     * A page of published products.
     *
     * [query] is matched server-side against name and description. Paging is
     * limit/offset to match the endpoint; the server clamps an over-large limit
     * rather than rejecting it, so a bad caller degrades instead of failing.
     */
    suspend fun products(
        categorySlug: String? = null,
        featuredOnly: Boolean = false,
        query: String? = null,
        limit: Int = 24,
        offset: Int = 0,
    ): ProductListResponse = api.get("/catalog/products") {
        categorySlug?.let { parameter("category", it) }
        if (featuredOnly) parameter("featured", "1")
        query?.takeIf { it.isNotBlank() }?.let { parameter("q", it) }
        parameter("limit", limit)
        parameter("offset", offset)
    }

    /**
     * One product, with its media, variants and approved reviews.
     *
     * Composed into a single response by the server precisely so the product
     * screen is one round trip rather than three on a phone network.
     */
    suspend fun product(slug: String): ProductDetailResponse =
        api.get("/catalog/products/$slug")

    suspend fun categories(): CategoryListResponse = api.get("/catalog/categories")
}
