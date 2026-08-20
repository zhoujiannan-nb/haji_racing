package com.haji.racing.domain.repository

import com.haji.racing.domain.model.ProfileStats
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.model.TrackStats
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun getAllTracks(): Flow<List<Track>>
    suspend fun getTrackByUid(uid: String): Track?
    suspend fun saveTrack(track: Track)
    suspend fun deleteTrack(uid: String)
    fun getTrackStats(): Flow<List<TrackStats>>
}

interface RecordingRepository {
    fun getAllRecordings(): Flow<List<Recording>>
    fun getRecordingsForTrack(trackUid: String): Flow<List<Recording>>
    suspend fun getRecordingByUid(uid: String): Recording?
    suspend fun saveRecording(recording: Recording)
    suspend fun deleteRecording(uid: String)
    suspend fun getLeaderboardForTrack(trackUid: String): List<Recording>
    suspend fun getBestForTrack(trackUid: String): Recording?
    suspend fun getProfileStats(): ProfileStats
    /** 清空所有记录（含轨迹点） */
    suspend fun clearAll()
}
