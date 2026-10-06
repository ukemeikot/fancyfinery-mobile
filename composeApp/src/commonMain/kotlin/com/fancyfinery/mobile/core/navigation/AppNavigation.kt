package com.fancyfinery.mobile.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.fancyfinery.mobile.core.platform.UrlOpener
import com.fancyfinery.mobile.features.account.presentation.AccountScreen
import com.fancyfinery.mobile.features.account.presentation.OrderDetailScreen
import com.fancyfinery.mobile.features.account.presentation.OrderDetailViewModel
import com.fancyfinery.mobile.features.auth.presentation.AuthScreen
import com.fancyfinery.mobile.features.cart.data.CartRepository
import com.fancyfinery.mobile.features.catalog.presentation.CatalogScreen
import com.fancyfinery.mobile.features.checkout.presentation.CheckoutOutcome
import com.fancyfinery.mobile.features.checkout.presentation.CheckoutScreen
import com.fancyfinery.mobile.features.home.presentation.HomeScreen
import com.fancyfinery.mobile.features.onboarding.presentation.OnboardingScreen
import com.fancyfinery.mobile.features.policy.presentation.PolicyScreen
import com.fancyfinery.mobile.features.product.presentation.ProductScreen
import com.fancyfinery.mobile.features.product.presentation.ProductViewModel
import com.fancyfinery.mobile.features.splash.SplashScreen
import com.fancyfinery.mobile.features.wishlist.presentation.WishlistScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The app shell: a back stack, plus the bottom bar.
 *
 * Navigation 3 hands you the stack as a plain list rather than hiding it behind
 * a controller, which makes one distinction visible that is easy to get wrong:
 * going FORWARD is `add`, while finishing a flow is `clear` then `add`. Splash,
 * onboarding and sign-in all use the second form — a customer must not be able
 * to press Back into a splash screen or into a sign-in they just completed.
 *
 * The bar is hidden for the full-screen flows and for pushed screens (product,
 * checkout, order, policy), which want the whole display and have their own
 * back affordance.
 */
@Composable
fun AppNavigation(startDestination: AppDestination) {
    val backStack = remember { mutableStateListOf(startDestination) }
    val cart = koinInject<CartRepository>()
    val urlOpener = koinInject<UrlOpener>()
    val bagCount by cart.observeCount().collectAsStateWithLifecycle(initialValue = 0)

    fun replaceAll(destination: AppDestination) {
        backStack.clear()
        backStack.add(destination)
    }

    fun push(destination: AppDestination) = backStack.add(destination)

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    /**
     * Switching tabs REPLACES the root rather than pushing.
     *
     * Pushing would make Back walk through every tab the customer has ever
     * visited, which is the most common complaint about hand-rolled bottom bars.
     */
    fun selectTab(destination: AppDestination) {
        if (TopLevelTab.of(backStack.lastOrNull() ?: destination) == TopLevelTab.of(destination) &&
            backStack.size == 1
        ) {
            return
        }
        replaceAll(destination)
    }

    val current = backStack.lastOrNull()
    val activeTab = current?.let { TopLevelTab.of(it) }
    val showBottomBar = activeTab != null

    Scaffold(
        /**
         * The shell consumes NO window insets.
         *
         * Its `NavigationBar` already handles the system navigation bar for
         * itself, and leaving the insets unconsumed is what lets a PUSHED
         * screen's own bottom bar — add-to-bag, place-order — clear that
         * navigation bar. Consuming them here instead meant the inset was spent
         * without being used on those screens, and their action bars rendered
         * underneath the system buttons.
         *
         * Tabbed screens take the top inset only (see `tabScaffoldInsets`);
         * pushed screens take the default and handle both ends themselves.
         */
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TopLevelTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = { selectTab(tab.destination) },
                            icon = {
                                if (tab == TopLevelTab.Bag && bagCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$bagCount") } }) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = {
                                Text(tab.label, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { pop() },
            modifier = Modifier.padding(
                bottom = if (showBottomBar) padding.calculateBottomPadding() else 0.dp,
            ),
            entryProvider = entryProvider {

                entry<AppDestination.Splash> {
                    SplashScreen(onFinished = { replaceAll(AppDestination.Home) })
                }

                entry<AppDestination.Onboarding> {
                    OnboardingScreen(onComplete = { replaceAll(AppDestination.Home) })
                }

                entry<AppDestination.Auth> { destination ->
                    AuthScreen(
                        initialMode = destination.initialMode,
                        onAuthSuccess = { replaceAll(AppDestination.Account) },
                        onDismiss = { replaceAll(AppDestination.Home) },
                    )
                }

                entry<AppDestination.Home> {
                    HomeScreen(
                        onProductClick = { slug -> push(AppDestination.Product(slug)) },
                        onCategoryClick = { slug ->
                            replaceAll(AppDestination.Catalog(categorySlug = slug))
                        },
                        onSeeAll = { replaceAll(AppDestination.Catalog()) },
                    )
                }

                entry<AppDestination.Catalog> { destination ->
                    CatalogScreen(
                        initialCategorySlug = destination.categorySlug,
                        onProductClick = { slug -> push(AppDestination.Product(slug)) },
                    )
                }

                entry<AppDestination.Wishlist> {
                    WishlistScreen(
                        onProductClick = { slug -> push(AppDestination.Product(slug)) },
                        onSignIn = { push(AppDestination.Auth()) },
                        onBrowse = { replaceAll(AppDestination.Catalog()) },
                    )
                }

                entry<AppDestination.Bag> {
                    com.fancyfinery.mobile.features.cart.presentation.CartScreen(
                        onCheckout = { push(AppDestination.Checkout) },
                        onProductClick = { slug -> push(AppDestination.Product(slug)) },
                        onBrowse = { replaceAll(AppDestination.Catalog()) },
                    )
                }

                entry<AppDestination.Account> {
                    AccountScreen(
                        onSignIn = { push(AppDestination.Auth()) },
                        onOrderClick = { id -> push(AppDestination.Order(id)) },
                        onOpenPolicy = { title -> push(AppDestination.Policy(title)) },
                    )
                }

                entry<AppDestination.Product> { destination ->
                    val viewModel: ProductViewModel =
                        koinViewModel(key = destination.slug) {
                            parametersOf(destination.slug)
                        }
                    ProductScreen(
                        onBack = { pop() },
                        onNeedsAuth = { push(AppDestination.Auth()) },
                        onRequestColor = { _, _ -> push(AppDestination.Policy("Contact")) },
                        viewModel = viewModel,
                    )
                }

                entry<AppDestination.Checkout> {
                    CheckoutScreen(
                        onBack = { pop() },
                        onSignIn = { push(AppDestination.Auth()) },
                        onOutcome = { outcome ->
                            when (outcome) {
                                is CheckoutOutcome.Pay -> {
                                    urlOpener.open(outcome.url)
                                    replaceAll(AppDestination.Order(outcome.orderId))
                                }
                                is CheckoutOutcome.PayOnDelivery ->
                                    replaceAll(AppDestination.Order(outcome.orderId))
                            }
                        },
                    )
                }

                entry<AppDestination.Order> { destination ->
                    val viewModel: OrderDetailViewModel =
                        koinViewModel(key = destination.orderId) {
                            parametersOf(destination.orderId)
                        }
                    OrderDetailScreen(
                        onBack = {
                            // An order reached from checkout has no history
                            // behind it, so Back goes to the account rather
                            // than out of the app.
                            if (backStack.size > 1) pop() else replaceAll(AppDestination.Account)
                        },
                        onPay = { _, url -> urlOpener.open(url) },
                        viewModel = viewModel,
                    )
                }

                entry<AppDestination.Policy> { destination ->
                    PolicyScreen(title = destination.title, onBack = { pop() })
                }
            },
        )
    }
}
