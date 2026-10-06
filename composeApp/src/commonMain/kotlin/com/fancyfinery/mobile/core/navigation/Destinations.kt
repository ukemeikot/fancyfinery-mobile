package com.fancyfinery.mobile.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.fancyfinery.mobile.features.auth.AuthMode
import kotlinx.serialization.Serializable

/**
 * Everywhere the app can be.
 *
 * `@Serializable` because Navigation 3 keys its back stack on these values; a
 * destination that cannot be serialised cannot be restored after the system
 * kills the process, which on Android is routine rather than exceptional.
 *
 * Products are addressed by SLUG rather than id, matching the website's URLs,
 * which is what makes `/products/ivory-bubble-hem-mini-dress` from an email or a
 * share sheet resolvable without a lookup table.
 */
@Serializable
sealed class AppDestination {

    @Serializable
    data object Splash : AppDestination()

    @Serializable
    data object Onboarding : AppDestination()

    @Serializable
    data class Auth(val initialMode: AuthMode = AuthMode.LOGIN) : AppDestination()

    // --- Tabbed sections ---------------------------------------------------

    @Serializable
    data object Home : AppDestination()

    @Serializable
    data class Catalog(val categorySlug: String? = null) : AppDestination()

    @Serializable
    data object Wishlist : AppDestination()

    @Serializable
    data object Bag : AppDestination()

    @Serializable
    data object Account : AppDestination()

    // --- Pushed screens ----------------------------------------------------

    @Serializable
    data class Product(val slug: String) : AppDestination()

    @Serializable
    data object Checkout : AppDestination()

    @Serializable
    data class Order(val orderId: String) : AppDestination()

    /** Reached from the emailed reset link. The token is the credential. */
    @Serializable
    data class ResetPassword(val token: String) : AppDestination()

    /** About / Contact / Shipping / Privacy / Terms, by title. */
    @Serializable
    data class Policy(val title: String) : AppDestination()
}

/**
 * The bottom bar's sections.
 *
 * Five: home, shop, saved, bag, account. The website separates its front page
 * from its catalogue and the app follows, because they answer different
 * questions — "show me the house" versus "let me find a thing" — and collapsing
 * them would mean either losing the editorial front page or burying search.
 */
enum class TopLevelTab(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector,
) {
    Home(AppDestination.Home, "Home", Icons.Outlined.Home),
    Shop(AppDestination.Catalog(), "Shop", Icons.Outlined.Storefront),
    Saved(AppDestination.Wishlist, "Saved", Icons.Outlined.FavoriteBorder),
    Bag(AppDestination.Bag, "Bag", Icons.Outlined.ShoppingBag),
    Account(AppDestination.Account, "Account", Icons.Outlined.Person);

    companion object {
        /**
         * Which tab a destination belongs to, or null if it is not tabbed.
         *
         * Catalog matches on TYPE rather than equality: browsing a category is
         * still the Shop tab, and comparing by value would unselect the tab the
         * moment a category filter was applied.
         */
        fun of(destination: AppDestination): TopLevelTab? = when (destination) {
            is AppDestination.Home -> Home
            is AppDestination.Catalog -> Shop
            is AppDestination.Wishlist -> Saved
            is AppDestination.Bag -> Bag
            is AppDestination.Account -> Account
            else -> null
        }
    }
}
