package com.fancyfinery.mobile.features.auth.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("user") val user: UserDto,
)

