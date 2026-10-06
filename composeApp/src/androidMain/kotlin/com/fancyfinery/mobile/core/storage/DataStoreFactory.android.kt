package com.fancyfinery.mobile.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fancyfinery")

actual fun createPreferenceStore(context: Any?): PreferenceStore {
    requireNotNull(context) { "Android context required" }
    // applicationContext for the same reason as the database: an Activity may
    // be handed in, and DataStore outlives it.
    return DataStorePreferenceStore((context as Context).applicationContext.dataStore)
}

private class DataStorePreferenceStore(private val dataStore: DataStore<Preferences>) :
    PreferenceStore {
    override suspend fun get(key: String): String? =
        dataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun put(key: String, value: String?) {
        dataStore.edit { prefs ->
            if (value != null) prefs[stringPreferencesKey(key)] = value
            else prefs.remove(stringPreferencesKey(key))
        }
    }
}
