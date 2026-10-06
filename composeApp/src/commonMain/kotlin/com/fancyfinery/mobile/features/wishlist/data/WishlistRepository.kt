package com.fancyfinery.mobile.features.wishlist.data

import com.fancyfinery.mobile.core.network.ApiClient
import com.fancyfinery.mobile.core.network.ApiErrorCode
import com.fancyfinery.mobile.core.network.ApiException
import com.fancyfinery.mobile.core.session.SessionStore
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
data class WishlistItemDto(
    val productId: String,
    val slug: String,
    val name: String,
    val price: com.fancyfinery.mobile.features.catalog.data.remote.dto.MoneyDto,
    val imageUrl: String? = null,
)

@Serializable
data class WishlistResponse(val items: List<WishlistItemDto> = emptyList())

@Serializable
private data class SaveRequest(val productId: String)

/**
 * Saved pieces.
 *
 * Server-side, unlike the bag, and deliberately so: a wishlist is account data
 * that should follow a customer between the app and the website, whereas a bag
 * is a working list tied to the device they are shopping on.
 *
 * That means saving requires an account. Rather than let every call site
 * discover that through a 401, [toggle] reports [ToggleResult.NeedsAuth] so the
 * UI can offer sign-in at the moment the customer actually wanted something.
 *
 * Ids are cached in memory after the first load so a grid of cards can render
 * its hearts without a request per card.
 */
class WishlistRepository(
    private val api: ApiClient,
    private val session: SessionStore,
) {

    private var cachedIds: Set<String>? = null

    enum class ToggleResult { Saved, Removed, NeedsAuth, Failed }

    suspend fun list(): WishlistResponse {
        if (!session.isSignedIn) return WishlistResponse()
        return api.get<WishlistResponse>("/account/wishlist").also { response ->
            cachedIds = response.items.map { it.productId }.toSet()
        }
    }

    suspend fun savedIds(): Set<String> {
        cachedIds?.let { return it }
        if (!session.isSignedIn) return emptySet()
        return runCatching { list().items.map { it.productId }.toSet() }
            .getOrDefault(emptySet())
    }

    suspend fun isSaved(productId: String): Boolean = savedIds().contains(productId)

    suspend fun toggle(productId: String, currentlySaved: Boolean): ToggleResult {
        if (!session.isSignedIn) return ToggleResult.NeedsAuth

        return runCatching {
            if (currentlySaved) {
                api.delete<Map<String, Boolean>>("/account/wishlist") {
                    parameter("productId", productId)
                }
                cachedIds = cachedIds?.minus(productId)
                ToggleResult.Removed
            } else {
                api.post<Map<String, Boolean>>(
                    "/account/wishlist",
                    SaveRequest(productId = productId),
                )
                cachedIds = cachedIds?.plus(productId) ?: setOf(productId)
                ToggleResult.Saved
            }
        }.getOrElse { e ->
            if ((e as? ApiException)?.code == ApiErrorCode.Unauthenticated) {
                ToggleResult.NeedsAuth
            } else {
                ToggleResult.Failed
            }
        }
    }

    suspend fun remove(productId: String) {
        runCatching {
            api.delete<Map<String, Boolean>>("/account/wishlist") {
                parameter("productId", productId)
            }
            cachedIds = cachedIds?.minus(productId)
        }
    }

    /** Called on sign-out, so the next account does not inherit these hearts. */
    fun forget() {
        cachedIds = null
    }
}
