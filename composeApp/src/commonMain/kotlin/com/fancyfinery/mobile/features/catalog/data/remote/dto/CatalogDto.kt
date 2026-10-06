package com.fancyfinery.mobile.features.catalog.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shapes for the catalogue endpoints, mirroring the server's `_lib/dto.ts`.
 *
 * (Written without a slash-star glob in this comment on purpose: Kotlin nests
 * block comments, so a stray one inside KDoc opens a comment that is never
 * closed and the whole file fails to parse.)
 *
 * Every nullable field here is nullable on the server too. Nothing is marked
 * optional to paper over a response the app did not expect — if a field goes
 * missing the decode should fail loudly in development rather than render a
 * blank price in a customer's hand.
 */

/**
 * An amount, pre-converted and pre-formatted by the server.
 *
 * The app never does money arithmetic or formatting. [amount] is minor units
 * for summing a cart; [formatted] is what gets displayed, produced by the same
 * `formatMinor` the website's price tags use — so a price in the app and the
 * same price in a browser cannot disagree about spacing, symbol or grouping.
 */
@Serializable
data class MoneyDto(
    val amount: Long,
    val currency: String,
    val formatted: String,
)

@Serializable
data class ImageDto(
    val id: String,
    val url: String,
    val alt: String? = null,
    val sortOrder: Int = 0,
    val mediaType: String = "image",
) {
    val isVideo: Boolean get() = mediaType == "video"
}

@Serializable
data class VariantDto(
    val id: String,
    val size: String? = null,
    val color: String? = null,
    val sku: String? = null,
    val stockQty: Int = 0,
    val inStock: Boolean = false,
)

@Serializable
data class ProductSummaryDto(
    val id: String,
    val name: String,
    val slug: String,
    val price: MoneyDto,
    val categoryId: String? = null,
    val featured: Boolean = false,
    val image: ImageDto? = null,
    /** Null when nobody has rated it — not the same as an average of zero. */
    val rating: Double? = null,
    val ratingCount: Int = 0,
)

@Serializable
data class ModelDto(
    val heightCm: Int,
    val weightKg: Int,
    val size: String,
)

@Serializable
data class ProductDetailDto(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val price: MoneyDto,
    val categoryId: String? = null,
    val featured: Boolean = false,
    val rating: Double? = null,
    val ratingCount: Int = 0,
    val images: List<ImageDto> = emptyList(),
    val variants: List<VariantDto> = emptyList(),
    val inStock: Boolean = false,
    val fitType: String = "regular",
    val weightGrams: Int = 0,
    /** Present only when height, weight and size are all recorded. */
    val model: ModelDto? = null,
)

@Serializable
data class ReviewDto(
    val id: String,
    val rating: Int,
    val title: String? = null,
    val body: String,
    val authorName: String,
    /** Left by someone who actually bought it — the app badges these. */
    val verified: Boolean = false,
    val fitFeedback: String? = null,
    val helpfulCount: Int = 0,
    val createdAt: String,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val sortOrder: Int = 0,
)

// --- Response envelopes (the `data` payload of each endpoint) ---------------

@Serializable
data class ProductListResponse(
    val items: List<ProductSummaryDto> = emptyList(),
    /** A fact about the query, not about how many survived a search filter. */
    val hasMore: Boolean = false,
    val limit: Int = 0,
    val offset: Int = 0,
    val currency: String = "NGN",
)

@Serializable
data class ProductDetailResponse(
    val product: ProductDetailDto,
    val reviews: List<ReviewDto> = emptyList(),
)

@Serializable
data class CategoryListResponse(
    val items: List<CategoryDto> = emptyList(),
)
