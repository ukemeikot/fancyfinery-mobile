package com.fancyfinery.mobile.core.di

import com.fancyfinery.mobile.features.auth.di.authModule
import com.fancyfinery.mobile.features.home.di.homeModule
import com.fancyfinery.mobile.features.onboarding.di.onboardingModule
import org.koin.dsl.KoinAppDeclaration

fun appDeclaration(context: Any? = null): KoinAppDeclaration = {
    modules(
        coreModule(context = context),
        preferencesModule(context = context),
        onboardingModule(),
        authModule(),
        homeModule(),
    )
}
