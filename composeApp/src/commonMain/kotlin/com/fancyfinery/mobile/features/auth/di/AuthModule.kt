package com.fancyfinery.mobile.features.auth.di

import com.fancyfinery.mobile.features.auth.AuthMode
import com.fancyfinery.mobile.features.auth.data.AuthRepository
import com.fancyfinery.mobile.features.auth.data.remote.AuthApi
import com.fancyfinery.mobile.features.auth.presentation.AuthViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun authModule(): Module = module {
    singleOf(::AuthApi)
    singleOf(::AuthRepository)
    viewModel { params ->
        AuthViewModel(
            get(),
            initialMode = params.getOrNull() ?: AuthMode.LOGIN,
        )
    }
}
