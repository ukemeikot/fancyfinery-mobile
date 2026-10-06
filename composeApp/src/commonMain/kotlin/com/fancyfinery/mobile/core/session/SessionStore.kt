package com.fancyfinery.mobile.core.session

import com.fancyfinery.mobile.core.storage.PreferenceStore
import com.fancyfinery.mobile.core.storage.PreferencesKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The signed-in session's bearer token, and the shopper's chosen currency.
 *
 * Both are read on almost every HTTP call, which is why they are cached in
 * memory behind a [StateFlow] rather than read from disk per request: the Ktor
 * request pipeline cannot suspend on a DataStore read for every call without
 * adding latency to all of them.
 *
 * [load] must run once at startup before the first request, so that a returning
 * customer's token is in memory by the time anything asks for it. `App.kt` does
 * that while deciding which screen to open.
 *
 * The token is a Better Auth session token. It is not a JWT and carries no
 * claims the app can read — it names a row in `auth_session`, which is what
 * makes signing out genuinely revoke it server-side rather than only discarding
 * the client's copy.
 */
class SessionStore(private val store: PreferenceStore) {

    private val _token = MutableStateFlow<String?>(null)

    /** Null when signed out. Collected by the UI to decide what to show. */
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _currency = MutableStateFlow(DEFAULT_CURRENCY)
    val currency: StateFlow<String> = _currency.asStateFlow()

    val isSignedIn: Boolean get() = _token.value != null

    /** Hydrate from disk. Call once, before the first request. */
    suspend fun load() {
        _token.value = store.get(PreferencesKeys.AUTH_TOKEN)
        _currency.value = store.get(PreferencesKeys.CURRENCY) ?: DEFAULT_CURRENCY
    }

    suspend fun saveToken(value: String) {
        _token.value = value
        store.put(PreferencesKeys.AUTH_TOKEN, value)
    }

    /**
     * Forget the session locally.
     *
     * Called both on a deliberate sign-out and when the server answers 401 — a
     * token the server no longer honours is worth nothing, and keeping it would
     * leave the app showing a signed-in shell that fails every request.
     */
    suspend fun clear() {
        _token.value = null
        store.put(PreferencesKeys.AUTH_TOKEN, null)
        store.put(PreferencesKeys.USER_ID, null)
    }

    suspend fun saveUserId(id: String) = store.put(PreferencesKeys.USER_ID, id)

    suspend fun userId(): String? = store.get(PreferencesKeys.USER_ID)

    /**
     * Change the currency the shopper is browsing — and will be charged — in.
     *
     * Persisted because it has to survive a relaunch: a customer who chose
     * pounds should not find the catalogue priced in naira the next morning.
     */
    suspend fun setCurrency(code: String) {
        _currency.value = code
        store.put(PreferencesKeys.CURRENCY, code)
    }

    companion object {
        const val DEFAULT_CURRENCY = "NGN"
    }
}
