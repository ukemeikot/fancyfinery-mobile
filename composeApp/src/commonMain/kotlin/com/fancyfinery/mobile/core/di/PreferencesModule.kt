package com.fancyfinery.mobile.core.di

import com.fancyfinery.mobile.core.storage.createPreferenceStore
import com.fancyfinery.mobile.features.auth.data.local.AuthPreferences
import com.fancyfinery.mobile.features.onboarding.data.OnboardingPreferences
import org.koin.core.module.Module
import org.koin.dsl.module

fun preferencesModule(context: Any?): Module = module {
    single { createPreferenceStore(context) }
    single { OnboardingPreferences(get()) }
    single { AuthPreferences(get()) }
}
