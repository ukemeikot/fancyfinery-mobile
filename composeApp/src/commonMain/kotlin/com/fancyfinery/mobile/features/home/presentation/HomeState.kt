package com.fancyfinery.mobile.features.home.presentation

import com.fancyfinery.mobile.features.auth.domain.model.User

data class HomeState(
    val user: User? = null,
    val isLoading: Boolean = true,
)