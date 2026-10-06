package com.fancyfinery.mobile.core.di

import com.fancyfinery.mobile.core.network.ApiClient
import com.fancyfinery.mobile.features.auth.di.authModule
import com.fancyfinery.mobile.features.catalog.di.catalogModule
import com.fancyfinery.mobile.features.onboarding.di.onboardingModule
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Every Koin module the app runs with.
 *
 * One module per feature, registered here. [ApiClient] sits in its own tiny
 * module rather than in `coreModule` only because it depends on the configured
 * `HttpClient` that `coreModule` builds, and keeping the declaration adjacent
 * to that dependency makes the ordering obvious.
 */
fun appDeclaration(context: Any? = null): KoinAppDeclaration = {
    modules(
        coreModule(context = context),
        preferencesModule(context = context),
        apiModule(),
        onboardingModule(),
        authModule(),
        catalogModule(),
    )
}

private fun apiModule() = module {
    single { ApiClient(get()) }
}
