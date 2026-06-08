package com.haji.racing.data.local.db.dao

import androidx.room.*
import com.haji.racing.data.local.db.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE trackUid = :trackUid ORDER BY createdAt DESC")
    fun getRecordingsForTrack(trackUid: String): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE trackUid = :trackUid ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestRecordingForTrack(trackUid: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE uid = :uid")
    suspend fun getRecordingByUid(uid: String): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE uid = :uid")
    suspend fun deleteRecording(uid: String)

    @Query("SELECT * FROM recordings WHERE isSynced = 0")
    suspend fun getUnsyncedRecordings(): List<RecordingEntity>

    @Query("SELECT SUM(totalDistance) FROM recordings WHERE userUid = :userUid")
    suspend fun getTotalDistanceForUser(userUid: String): Double?
}
