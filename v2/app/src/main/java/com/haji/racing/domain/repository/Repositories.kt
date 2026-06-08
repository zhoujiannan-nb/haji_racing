package com.haji.racing.domain.repository

import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.model.User
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun getAllTracks(): Flow<List<Track>>
    fun getTracksByType(type: String): Flow<List<Track>>
    suspend fun getTrackByUid(uid: String): Track?
    suspend fun saveTrack(track: Track)
    suspend fun deleteTrack(uid: String)
}

interface RecordingRepository {
    fun getAllRecordings(): Flow<List<Recording>>
    fun getRecordingsForTrack(trackUid: String): Flow<List<Recording>>
    suspend fun getLatestRecordingForTrack(trackUid: String): Recording?
    suspend fun getRecordingByUid(uid: String): Recording?
    suspend fun saveRecording(recording: Recording)
    suspend fun deleteRecording(uid: String)
    suspend fun getTotalDistanceForUser(userUid: String): Double
}

interface UserRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun getUserByUid(uid: String): User?
    suspend fun saveUser(user: User)
    suspend fun updateUser(user: User)
    suspend fun deleteUser(uid: String)
}
