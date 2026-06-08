package com.haji.racing.data.local.db.dao

import androidx.room.*
import com.haji.racing.data.local.db.entity.TrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE trackUid = :trackUid ORDER BY sequenceIndex")
    fun getPointsForTrack(trackUid: String): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points WHERE trackUid = :trackUid ORDER BY sequenceIndex")
    suspend fun getPointsForTrackSync(trackUid: String): List<TrackPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Query("DELETE FROM track_points WHERE trackUid = :trackUid")
    suspend fun deletePointsForTrack(trackUid: String)
}
