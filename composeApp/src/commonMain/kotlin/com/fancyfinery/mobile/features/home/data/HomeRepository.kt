package com.fancyfinery.mobile.features.home.data

import com.fancyfinery.mobile.core.network.ApiClient
import com.fancyfinery.mobile.features.catalog.data.remote.dto.CategoryDto
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductSummaryDto
import kotlinx.serialization.Serializable

@Serializable
data class LookbookEntryDto(
    val slug: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String,
)

@Serializable
data class HomeReviewDto(
    val id: String,
    val rating: Int,
    val title: String? = null,
    val body: String,
    val authorName: String,
    val verified: Boolean = false,
    val productName: String,
    val productSlug: String,
    val createdAt: String,
)

@Serializable
data class HomeResponse(
    val currency: String = "NGN",
    val featured: List<ProductSummaryDto> = emptyList(),
    val newArrivals: List<ProductSummaryDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val lookbook: List<LookbookEntryDto> = emptyList(),
    val reviews: List<HomeReviewDto> = emptyList(),
)

@Serializable
data class LookbookResponse(val items: List<LookbookEntryDto> = emptyList())

@Serializable
private data class NewsletterRequest(
    val email: String,
    val name: String? = null,
    /** Honeypot. Always empty from the app; bots fill it in on the web form. */
    val website: String = "",
)

@Serializable
data class NewsletterResponse(val kind: String = "created", val message: String = "")

/**
 * The home screen's content, and the Privé Circle signup.
 *
 * One call fetches the whole home feed. The server composes it from five
 * sources precisely so this is a single round trip rather than five sequential
 * ones over a mobile connection.
 */
class HomeRepository(private val api: ApiClient) {

    suspend fun home(): HomeResponse = api.get("/home")

    suspend fun lookbook(): LookbookResponse = api.get("/lookbook")

    suspend fun joinPriveCircle(email: String, name: String?): NewsletterResponse =
        api.post("/newsletter", NewsletterRequest(email = email.trim(), name = name?.trim()))
}
