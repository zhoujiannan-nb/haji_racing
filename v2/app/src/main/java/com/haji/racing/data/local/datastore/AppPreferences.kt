package com.haji.racing.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "haji_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_USER_UID = stringPreferencesKey("user_uid")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_SELECTED_TRACK_UID = stringPreferencesKey("selected_track_uid")
    }

    val userUid: Flow<String?> = context.dataStore.data.map { it[KEY_USER_UID] }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }
    val selectedTrackUid: Flow<String?> = context.dataStore.data.map { it[KEY_SELECTED_TRACK_UID] }

    suspend fun setUserUid(uid: String) {
        context.dataStore.edit { it[KEY_USER_UID] = uid }
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { it[KEY_IS_LOGGED_IN] = loggedIn }
    }

    suspend fun setSelectedTrackUid(uid: String?) {
        context.dataStore.edit {
            if (uid != null) it[KEY_SELECTED_TRACK_UID] = uid
            else it.remove(KEY_SELECTED_TRACK_UID)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
