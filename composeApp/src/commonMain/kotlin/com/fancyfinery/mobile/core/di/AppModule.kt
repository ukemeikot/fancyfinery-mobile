package com.fancyfinery.mobile.core.di

import com.fancyfinery.mobile.core.database.AppDatabase
import com.fancyfinery.mobile.core.network.ApiClient
import com.fancyfinery.mobile.features.account.data.AccountRepository
import com.fancyfinery.mobile.features.account.presentation.AccountViewModel
import com.fancyfinery.mobile.features.account.presentation.OrderDetailViewModel
import com.fancyfinery.mobile.features.auth.di.authModule
import com.fancyfinery.mobile.features.cart.data.CartRepository
import com.fancyfinery.mobile.features.cart.presentation.CartViewModel
import com.fancyfinery.mobile.features.catalog.di.catalogModule
import com.fancyfinery.mobile.features.checkout.data.CheckoutRepository
import com.fancyfinery.mobile.features.checkout.presentation.CheckoutViewModel
import com.fancyfinery.mobile.features.home.data.HomeRepository
import com.fancyfinery.mobile.features.home.presentation.HomeViewModel
import com.fancyfinery.mobile.features.onboarding.di.onboardingModule
import com.fancyfinery.mobile.features.product.presentation.ProductViewModel
import com.fancyfinery.mobile.features.wishlist.data.WishlistRepository
import com.fancyfinery.mobile.features.wishlist.presentation.WishlistViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Every Koin module the app runs with.
 *
 * Feature modules stay small and local; this is the only place that knows the
 * full set. Two view models take a runtime parameter — the product slug and the
 * order id — so they are declared with an explicit `viewModel { params -> }`
 * rather than a constructor reference.
 */
fun appDeclaration(context: Any? = null): KoinAppDeclaration = {
    modules(
        coreModule(context = context),
        preferencesModule(context = context),
        apiModule(),
        onboardingModule(),
        authModule(),
        catalogModule(),
        shopModule(),
    )
}

private fun apiModule() = module {
    single { ApiClient(get()) }
}

private fun shopModule() = module {
    // DAOs that live on the shared database.
    single { get<AppDatabase>().cartDao() }
    single { get<AppDatabase>().recentlyViewedDao() }

    singleOf(::CartRepository)
    singleOf(::WishlistRepository)
    singleOf(::AccountRepository)
    singleOf(::CheckoutRepository)
    singleOf(::HomeRepository)

    viewModelOf(::HomeViewModel)
    viewModelOf(::CartViewModel)
    viewModelOf(::WishlistViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::CheckoutViewModel)

    // Parameterised: the screen supplies the slug / id it was opened for.
    viewModel { params ->
        ProductViewModel(
            slug = params.get(),
            catalog = get(),
            cart = get(),
            wishlist = get(),
            recentlyViewed = get(),
        )
    }
    viewModel { params ->
        OrderDetailViewModel(
            orderId = params.get(),
            account = get(),
            checkout = get(),
        )
    }
}
