package com.fancyfinery.mobile.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import java.io.File

/**
 * Preferences for the desktop preview host.
 *
 * Kept beside the preview database in `~/.fancyfinery-preview`, so clearing the
 * previewed app's state — signing out, replaying onboarding — is deleting one
 * folder rather than hunting two paths.
 */
actual fun createPreferenceStore(context: Any?): PreferenceStore {
    val appDir = File(System.getProperty("user.home"), ".fancyfinery-preview")
        .also { it.mkdirs() }
    val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = { File(appDir, "fancyfinery.preferences_pb").absolutePath.toPath() },
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
