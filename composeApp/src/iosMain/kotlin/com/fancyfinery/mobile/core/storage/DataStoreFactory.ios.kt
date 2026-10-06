package com.fancyfinery.mobile.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun createPreferenceStore(context: Any?): PreferenceStore {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )!!.path!!
    val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = { "$documentDirectory/kotlin_starter.preferences_pb".toPath() },
    )
    return DataStorePreferenceStore(dataStore)
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
