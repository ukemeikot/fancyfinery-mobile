package com.fancyfinery.mobile.features.auth.data.remote

import com.fancyfinery.mobile.features.auth.data.remote.dto.LoginRequest
import com.fancyfinery.mobile.features.auth.data.remote.dto.LoginResponse
import com.fancyfinery.mobile.features.auth.data.remote.dto.RegisterRequest
import com.fancyfinery.mobile.features.auth.data.remote.dto.RegisterResponse
import com.fancyfinery.mobile.features.auth.data.remote.dto.UserDto
import kotlinx.coroutines.delay

class AuthApi {

    suspend fun login(request: LoginRequest): LoginResponse {
        delay(800)
        if (request.email != DEMO_EMAIL || request.password != DEMO_PASSWORD) {
            throw IllegalArgumentException("Invalid email or password")
        }
        return LoginResponse(
            accessToken = "demo_token",
            user = UserDto(id = "user_demo", email = DEMO_EMAIL, name = "Jeffery Orazulike"),
        )
    }

    suspend fun register(request: RegisterRequest): RegisterResponse {
        delay(800)
        return RegisterResponse(
            accessToken = "demo_token_${request.email}",
            user = UserDto(
                id = "user_${request.email.hashCode()}",
                email = request.email,
                name = request.name,
            ),
        )
    }

    companion object {
        const val DEMO_EMAIL = "demo@example.com"
        const val DEMO_PASSWORD = "Password1"
    }
}
