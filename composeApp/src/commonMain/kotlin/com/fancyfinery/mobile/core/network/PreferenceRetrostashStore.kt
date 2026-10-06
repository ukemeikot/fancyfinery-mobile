package com.fancyfinery.mobile.core.network

import dev.logickoder.retrostash.core.RetrostashStore
import com.fancyfinery.mobile.core.storage.PreferenceStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock

class PreferenceRetrostashStore(private val store: PreferenceStore) : RetrostashStore {
    private val mutex = Mutex()
    private var cache: MutableMap<String, CacheEntry>? = null

    private suspend fun loadCache(): MutableMap<String, CacheEntry> {
        cache?.let { return it }
        val json = store.get(CACHE_KEY)
        val loaded = if (json != null) {
            runCatching { cacheJson.decodeFromString(mapSerializer, json) }
                .getOrDefault(emptyMap())
                .toMutableMap()
        } else {
            mutableMapOf()
        }
        cache = loaded
        return loaded
    }

    private suspend fun saveCache(map: MutableMap<String, CacheEntry>) {
        store.put(CACHE_KEY, cacheJson.encodeToString(mapSerializer, map))
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun get(key: String): ByteArray? = mutex.withLock {
        val map = loadCache()
        val entry = map[key] ?: return@withLock null
        val now = Clock.System.now().toEpochMilliseconds()
        if (entry.expiry in 1..<now) {
            map.remove(key)
            saveCache(map)
            return@withLock null
        }
        Base64.decode(entry.data)
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun put(key: String, payload: ByteArray, maxAgeMs: Long) {
        mutex.withLock {
            val map = loadCache()
            val expiry =
                if (maxAgeMs > 0) Clock.System.now().toEpochMilliseconds() + maxAgeMs else 0L
            map[key] = CacheEntry(data = Base64.encode(payload), expiry = expiry)
            saveCache(map)
        }
    }

    override suspend fun invalidate(template: String) {
        mutex.withLock {
            val map = loadCache()
            val toRemove = map.keys.filter { key -> "|$template|" in key || key == template }
            if (toRemove.isNotEmpty()) {
                toRemove.forEach { map.remove(it) }
                saveCache(map)
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            cache = mutableMapOf()
            store.put(CACHE_KEY, null)
        }
    }

    private companion object {
        const val CACHE_KEY = "retrostash_cache"

        @Serializable
        private data class CacheEntry(val data: String, val expiry: Long)

        private val cacheJson = Json { ignoreUnknownKeys = true }
        private val mapSerializer = MapSerializer(String.serializer(), CacheEntry.serializer())
    }
}
