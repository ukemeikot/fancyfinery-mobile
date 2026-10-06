package com.fancyfinery.mobile.features.auth

import com.fancyfinery.mobile.features.auth.data.remote.AuthApi
import com.fancyfinery.mobile.features.auth.data.remote.dto.LoginRequest
import com.fancyfinery.mobile.features.auth.data.remote.dto.RegisterRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthApiTest {

    private val api = AuthApi()

    @Test
    fun loginSucceedsWithDemoCredentials() = runTest {
        val response =
            api.login(LoginRequest(email = AuthApi.DEMO_EMAIL, password = AuthApi.DEMO_PASSWORD))
        assertEquals(AuthApi.DEMO_EMAIL, response.user.email)
        assertEquals("demo_token", response.accessToken)
    }

    @Test
    fun loginFailsWithWrongEmail() = runTest {
        assertFailsWith<IllegalArgumentException> {
            api.login(LoginRequest(email = "wrong@example.com", password = AuthApi.DEMO_PASSWORD))
        }
    }

    @Test
    fun loginFailsWithWrongPassword() = runTest {
        assertFailsWith<IllegalArgumentException> {
            api.login(LoginRequest(email = AuthApi.DEMO_EMAIL, password = "wrongpass"))
        }
    }

    @Test
    fun registerAlwaysSucceeds() = runTest {
        val response = api.register(
            RegisterRequest(name = "Test User", email = "test@example.com", password = "pass123"),
        )
        assertEquals("test@example.com", response.user.email)
        assertEquals("Test User", response.user.name)
    }
}
