package com.haji.racing.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.haji.racing.data.local.db.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

/** 单条赛道的聚合统计（用于赛道卡片） */
data class TrackStatRow(
    val trackUid: String,
    val recordCount: Int,
    val bestTimeMs: Long?,
)

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE trackUid = :trackUid ORDER BY createdAt DESC")
    fun getRecordingsForTrack(trackUid: String): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE uid = :uid")
    suspend fun getRecordingByUid(uid: String): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE uid = :uid")
    suspend fun deleteRecording(uid: String)

    @Query("DELETE FROM recordings")
    suspend fun deleteAllRecordings()

    @Query("DELETE FROM recordings WHERE trackUid = :trackUid")
    suspend fun deleteRecordingsForTrack(trackUid: String)

    // ---- 榜单 / 统计 ----

    @Query(
        "SELECT * FROM recordings WHERE trackUid = :trackUid AND status = 'completed' " +
            "ORDER BY (endTime - startTime) ASC LIMIT 5"
    )
    suspend fun getLeaderboardForTrack(trackUid: String): List<RecordingEntity>

    @Query(
        "SELECT * FROM recordings WHERE trackUid = :trackUid AND status = 'completed' " +
            "ORDER BY (endTime - startTime) ASC LIMIT 1"
    )
    suspend fun getBestForTrack(trackUid: String): RecordingEntity?

    @Query(
        "SELECT trackUid, COUNT(*) AS recordCount, MIN(endTime - startTime) AS bestTimeMs " +
            "FROM recordings WHERE status = 'completed' GROUP BY trackUid"
    )
    fun getTrackStats(): Flow<List<TrackStatRow>>

    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM recordings WHERE status = 'completed'")
    suspend fun countCompleted(): Int

    @Query("SELECT COALESCE(SUM(totalDistance), 0) FROM recordings")
    suspend fun sumAllDistance(): Double

    @Query("SELECT COALESCE(SUM(endTime - startTime), 0) FROM recordings")
    suspend fun sumAllDuration(): Long

    @Query(
        "SELECT * FROM recordings WHERE status = 'completed' " +
            "ORDER BY (endTime - startTime) ASC LIMIT 1"
    )
    suspend fun getBestOverall(): RecordingEntity?
}
