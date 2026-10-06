package com.fancyfinery.mobile.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
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
 * Products are addressed by SLUG rather than id, matching the website's URLs.
 * That is what makes `/products/ivory-bubble-hem-mini-dress` — from an email, a
 * share sheet or a push — resolvable by the app without a lookup table.
 */
@Serializable
sealed class AppDestination {

    @Serializable
    data object Splash : AppDestination()

    @Serializable
    data object Onboarding : AppDestination()

    @Serializable
    data class Auth(val initialMode: AuthMode = AuthMode.LOGIN) : AppDestination()

    /** The four tabbed sections. */
    @Serializable
    data object Catalog : AppDestination()

    @Serializable
    data object Wishlist : AppDestination()

    @Serializable
    data object Bag : AppDestination()

    @Serializable
    data object Account : AppDestination()

    @Serializable
    data class Product(val slug: String) : AppDestination()
}

/**
 * The bottom bar's sections.
 *
 * Four, deliberately. Three wastes a bar people are used to reading; five on a
 * phone makes each target too narrow to hit reliably. These are the four a
 * shopper actually moves between — browse, saved, bag, account — and
 * everything else is reached from inside one of them.
 */
enum class TopLevelTab(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector,
) {
    Shop(AppDestination.Catalog, "Shop", Icons.Outlined.Storefront),
    Saved(AppDestination.Wishlist, "Saved", Icons.Outlined.FavoriteBorder),
    Bag(AppDestination.Bag, "Bag", Icons.Outlined.ShoppingBag),
    Account(AppDestination.Account, "Account", Icons.Outlined.Person);

    companion object {
        /** Which tab a destination belongs to, or null if it is not tabbed. */
        fun of(destination: AppDestination): TopLevelTab? =
            entries.firstOrNull { it.destination == destination }
    }
}
