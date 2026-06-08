package com.haji.racing.data.repository

import com.haji.racing.data.local.db.dao.TrackDao
import com.haji.racing.data.local.db.dao.TrackPointDao
import com.haji.racing.data.local.db.entity.TrackEntity
import com.haji.racing.data.local.db.entity.TrackPointEntity
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.model.TrackPoint
import com.haji.racing.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao,
) : TrackRepository {

    override fun getAllTracks(): Flow<List<Track>> =
        trackDao.getAllTracks().map { list -> list.map { it.toDomain() } }

    override fun getTracksByType(type: String): Flow<List<Track>> =
        trackDao.getTracksByType(type).map { list -> list.map { it.toDomain() } }

    override suspend fun getTrackByUid(uid: String): Track? {
        val entity = trackDao.getTrackByUid(uid) ?: return null
        val points = trackPointDao.getPointsForTrackSync(uid).map { TrackPoint(it.lat, it.lng, it.sequenceIndex) }
        return entity.toDomain().copy(points = points)
    }

    override suspend fun saveTrack(track: Track) {
        trackDao.insertTrack(track.toEntity())
        val points = track.points.mapIndexed { index, point ->
            TrackPointEntity(trackUid = track.uid, sequenceIndex = point.sequenceIndex, lat = point.lat, lng = point.lng)
        }
        if (points.isNotEmpty()) trackPointDao.insertPoints(points)
    }

    override suspend fun deleteTrack(uid: String) {
        trackPointDao.deletePointsForTrack(uid)
        trackDao.deleteTrack(uid)
    }

    private fun TrackEntity.toDomain() = Track(
        uid = uid, name = name, type = type,
        startLat = startLat, startLng = startLng, startFenceRadius = startFenceRadius,
        endLat = endLat, endLng = endLng, endFenceRadius = endFenceRadius,
        totalDistance = totalDistance, creatorUid = creatorUid, isSynced = isSynced,
        createdAt = createdAt, updatedAt = updatedAt,
    )

    private fun Track.toEntity() = TrackEntity(
        uid = uid, name = name, type = type,
        startLat = startLat, startLng = startLng, startFenceRadius = startFenceRadius,
        endLat = endLat, endLng = endLng, endFenceRadius = endFenceRadius,
        totalDistance = totalDistance, creatorUid = creatorUid, isSynced = isSynced,
        createdAt = createdAt, updatedAt = updatedAt,
    )
}
