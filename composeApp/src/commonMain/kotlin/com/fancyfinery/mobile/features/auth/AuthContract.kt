package com.fancyfinery.mobile.features.auth

import kotlinx.serialization.Serializable

typealias OnAuthSuccess = () -> Unit

@Serializable
enum class AuthMode { LOGIN, REGISTER }