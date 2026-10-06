package com.fancyfinery.mobile.features.catalog.di

import com.fancyfinery.mobile.features.catalog.data.CatalogRepository
import com.fancyfinery.mobile.features.catalog.presentation.CatalogViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun catalogModule(): Module = module {
    singleOf(::CatalogRepository)
    viewModelOf(::CatalogViewModel)
}
