package com.fancyfinery.mobile.core.storage

interface PreferenceStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String?)
}

expect fun createPreferenceStore(context: Any? = null): PreferenceStore
