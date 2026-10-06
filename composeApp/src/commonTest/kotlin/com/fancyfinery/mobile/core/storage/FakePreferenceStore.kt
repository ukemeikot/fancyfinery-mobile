package com.fancyfinery.mobile.core.storage

class FakePreferenceStore : PreferenceStore {
    private val map = mutableMapOf<String, String>()

    override suspend fun get(key: String): String? = map[key]

    override suspend fun put(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }

    fun clear() = map.clear()
}
