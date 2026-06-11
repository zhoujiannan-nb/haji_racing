package com.haji.racing.data.repository

import com.haji.racing.data.local.db.dao.TrackDao
import com.haji.racing.data.local.db.entity.TrackEntity
import com.haji.racing.domain.model.FencePoint
import com.haji.racing.domain.model.Track
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
    private val gson: Gson,
) : TrackRepository {

    override fun getAllTracks(): Flow<List<Track>> =
        trackDao.getAllTracks().map { list -> list.map { it.toDomain() } }

    override fun getTracksByType(type: String): Flow<List<Track>> =
        trackDao.getTracksByType(type).map { list -> list.map { it.toDomain() } }

    override suspend fun getTrackByUid(uid: String): Track? {
        val entity = trackDao.getTrackByUid(uid) ?: return null
        return entity.toDomain()
    }

    override suspend fun saveTrack(track: Track) {
        trackDao.insertTrack(track.toEntity())
    }

    override suspend fun deleteTrack(uid: String) {
        trackDao.deleteTrack(uid)
    }

    private fun TrackEntity.toDomain() = Track(
        uid = uid,
        name = name,
        type = type,
        startFencePoints = deserializeFencePoints(startFencePointsJson),
        endFencePoints = deserializeFencePoints(endFencePointsJson),
        totalDistance = totalDistance,
        creatorUid = creatorUid,
        isSynced = isSynced,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun Track.toEntity() = TrackEntity(
        uid = uid,
        name = name,
        type = type,
        startFencePointsJson = serializeFencePoints(startFencePoints),
        endFencePointsJson = serializeFencePoints(endFencePoints),
        totalDistance = totalDistance,
        creatorUid = creatorUid,
        isSynced = isSynced,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun serializeFencePoints(points: List<FencePoint>): String {
        return gson.toJson(points)
    }

    private fun deserializeFencePoints(json: String): List<FencePoint> {
        if (json.isBlank() || json == "[]") return emptyList()
        val type = object : TypeToken<List<FencePoint>>() {}.type
        return gson.fromJson(json, type)
    }
}
