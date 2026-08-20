package com.haji.racing.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haji.racing.data.local.db.entity.RecordingPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingPointDao {
    @Query("SELECT * FROM recording_points WHERE recordingUid = :recordingUid ORDER BY timestamp")
    fun getPointsForRecording(recordingUid: String): Flow<List<RecordingPointEntity>>

    @Query("SELECT * FROM recording_points WHERE recordingUid = :recordingUid ORDER BY timestamp")
    suspend fun getPointsForRecordingSync(recordingUid: String): List<RecordingPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: RecordingPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<RecordingPointEntity>)

    @Query("DELETE FROM recording_points WHERE recordingUid = :recordingUid")
    suspend fun deletePointsForRecording(recordingUid: String)

    @Query(
        "DELETE FROM recording_points WHERE recordingUid IN " +
            "(SELECT uid FROM recordings WHERE trackUid = :trackUid)"
    )
    suspend fun deletePointsForTrack(trackUid: String)

    @Query("DELETE FROM recording_points")
    suspend fun deleteAllPoints()
}
