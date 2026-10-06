package com.fancyfinery.mobile.core.navigation

import com.fancyfinery.mobile.features.auth.AuthMode
import kotlinx.serialization.Serializable

@Serializable
sealed class AppDestination {

    @Serializable
    data object Onboarding : AppDestination()

    @Serializable
    data class Auth(val initialMode: AuthMode = AuthMode.LOGIN) : AppDestination()

    @Serializable
    data object Home : AppDestination()
}
