package com.haji.racing.data.local.db.dao

import androidx.room.*
import com.haji.racing.data.local.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY createdAt DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE type = :type ORDER BY createdAt DESC")
    fun getTracksByType(type: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE uid = :uid")
    suspend fun getTrackByUid(uid: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE uid = :uid")
    suspend fun deleteTrack(uid: String)

    @Query("SELECT * FROM tracks WHERE isSynced = 0")
    suspend fun getUnsyncedTracks(): List<TrackEntity>
}
