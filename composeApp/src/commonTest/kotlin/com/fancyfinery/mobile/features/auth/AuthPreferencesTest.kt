package com.fancyfinery.mobile.features.auth

import com.fancyfinery.mobile.core.storage.FakePreferenceStore
import com.fancyfinery.mobile.features.auth.data.local.AuthPreferences
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthPreferencesTest {

    private val prefs = AuthPreferences(store = FakePreferenceStore())

    @Test
    fun noSessionInitially() = runTest {
        assertNull(prefs.getToken())
        assertNull(prefs.getUserId())
    }

    @Test
    fun saveSessionStoresTokenAndUserId() = runTest {
        prefs.saveSession(token = "tok123", userId = "u1")
        assertEquals("tok123", prefs.getToken())
        assertEquals("u1", prefs.getUserId())
    }

    @Test
    fun clearSessionRemovesTokenAndUserId() = runTest {
        prefs.saveSession(token = "tok123", userId = "u1")
        prefs.clearSession()
        assertNull(prefs.getToken())
        assertNull(prefs.getUserId())
    }

    @Test
    fun saveSessionOverwritesPreviousSession() = runTest {
        prefs.saveSession(token = "old", userId = "old-user")
        prefs.saveSession(token = "new", userId = "new-user")
        assertEquals("new", prefs.getToken())
        assertEquals("new-user", prefs.getUserId())
    }
}
