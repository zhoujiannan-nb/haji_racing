package com.haji.racing.data.repository

import com.haji.racing.data.local.db.dao.RecordingDao
import com.haji.racing.data.local.db.dao.RecordingPointDao
import com.haji.racing.data.local.db.dao.TrackDao
import com.haji.racing.data.local.db.entity.TrackEntity
import com.haji.racing.domain.model.FencePoint
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.model.TrackStats
import com.haji.racing.domain.repository.TrackRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao,
    private val recordingDao: RecordingDao,
    private val recordingPointDao: RecordingPointDao,
    private val gson: Gson,
) : TrackRepository {

    override fun getAllTracks(): Flow<List<Track>> =
        trackDao.getAllTracks().map { list -> list.map { it.toDomain() } }

    override suspend fun getTrackByUid(uid: String): Track? =
        trackDao.getTrackByUid(uid)?.toDomain()

    override suspend fun saveTrack(track: Track) {
        trackDao.insertTrack(track.toEntity())
    }

    override suspend fun deleteTrack(uid: String) {
        // 级联删除该赛道的所有记录与轨迹点
        recordingPointDao.deletePointsForTrack(uid)
        recordingDao.deleteRecordingsForTrack(uid)
        trackDao.deleteTrack(uid)
    }

    override fun getTrackStats(): Flow<List<TrackStats>> =
        recordingDao.getTrackStats().map { rows ->
            rows.map { row ->
                TrackStats(
                    trackUid = row.trackUid,
                    recordCount = row.recordCount,
                    bestTimeMs = row.bestTimeMs,
                )
            }
        }

    private fun TrackEntity.toDomain() = Track(
        uid = uid,
        name = name,
        description = description,
        startFencePoints = deserializeFencePoints(startFencePointsJson),
        endFencePoints = deserializeFencePoints(endFencePointsJson),
        totalDistance = totalDistance,
        creatorName = creatorName,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun Track.toEntity() = TrackEntity(
        uid = uid,
        name = name,
        description = description,
        startFencePointsJson = serializeFencePoints(startFencePoints),
        endFencePointsJson = serializeFencePoints(endFencePoints),
        totalDistance = totalDistance,
        creatorName = creatorName,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun serializeFencePoints(points: List<FencePoint>): String = gson.toJson(points)

    private fun deserializeFencePoints(json: String): List<FencePoint> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val type = object : TypeToken<List<FencePoint>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
