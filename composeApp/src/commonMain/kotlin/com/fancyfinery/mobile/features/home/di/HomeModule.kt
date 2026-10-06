package com.fancyfinery.mobile.features.home.di

import com.fancyfinery.mobile.features.home.presentation.HomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun homeModule(): Module = module {
    viewModel {
        HomeViewModel(get())
    }
}
