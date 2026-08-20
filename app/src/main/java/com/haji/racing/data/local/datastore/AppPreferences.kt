package com.haji.racing.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "haji_prefs")

/**
 * 本地偏好：昵称 + 默认赛道。无账号体系，全部本地。
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_NICKNAME = stringPreferencesKey("nickname")
        private val KEY_SELECTED_TRACK = stringPreferencesKey("selected_track_uid")
    }

    val nickname: Flow<String> = context.dataStore.data.map { it[KEY_NICKNAME] ?: "Haji" }

    val selectedTrackUid: Flow<String?> = context.dataStore.data.map { it[KEY_SELECTED_TRACK] }

    suspend fun setNickname(name: String) {
        context.dataStore.edit { it[KEY_NICKNAME] = name }
    }

    suspend fun setSelectedTrackUid(uid: String?) {
        context.dataStore.edit {
            if (uid != null) it[KEY_SELECTED_TRACK] = uid
            else it.remove(KEY_SELECTED_TRACK)
        }
    }
}
