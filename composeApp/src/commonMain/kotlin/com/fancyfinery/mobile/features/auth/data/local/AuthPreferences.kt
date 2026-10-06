package com.fancyfinery.mobile.features.auth.data.local

import com.fancyfinery.mobile.core.storage.PreferenceStore

class AuthPreferences(private val store: PreferenceStore) {
    suspend fun getToken(): String? = store.get("auth_token")
    suspend fun getUserId(): String? = store.get("user_id")

    suspend fun saveSession(token: String, userId: String) {
        store.put("auth_token", token)
        store.put("user_id", userId)
    }

    suspend fun clearSession() {
        store.put("auth_token", null)
        store.put("user_id", null)
    }
}
