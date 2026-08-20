package com.haji.racing.data.repository

import com.haji.racing.data.local.db.dao.RecordingDao
import com.haji.racing.data.local.db.dao.RecordingPointDao
import com.haji.racing.data.local.db.entity.RecordingEntity
import com.haji.racing.data.local.db.entity.RecordingPointEntity
import com.haji.racing.domain.model.ProfileStats
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.model.RecordingPoint
import com.haji.racing.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao,
    private val recordingPointDao: RecordingPointDao,
) : RecordingRepository {

    override fun getAllRecordings(): Flow<List<Recording>> =
        recordingDao.getAllRecordings().map { list -> list.map { it.toDomain() } }

    override fun getRecordingsForTrack(trackUid: String): Flow<List<Recording>> =
        recordingDao.getRecordingsForTrack(trackUid).map { list -> list.map { it.toDomain() } }

    override suspend fun getRecordingByUid(uid: String): Recording? {
        val entity = recordingDao.getRecordingByUid(uid) ?: return null
        val points = recordingPointDao.getPointsForRecordingSync(uid).map {
            RecordingPoint(it.timestamp, it.lat, it.lng, it.speed, it.distance)
        }
        return entity.toDomain().copy(points = points)
    }

    override suspend fun saveRecording(recording: Recording) {
        recordingDao.insertRecording(recording.toEntity())
        val points = recording.points.map { point ->
            RecordingPointEntity(
                recordingUid = recording.uid,
                timestamp = point.timestamp,
                lat = point.lat,
                lng = point.lng,
                speed = point.speed,
                distance = point.distance,
            )
        }
        if (points.isNotEmpty()) recordingPointDao.insertPoints(points)
    }

    override suspend fun deleteRecording(uid: String) {
        recordingPointDao.deletePointsForRecording(uid)
        recordingDao.deleteRecording(uid)
    }

    override suspend fun clearAll() {
        recordingPointDao.deleteAllPoints()
        recordingDao.deleteAllRecordings()
    }

    override suspend fun getLeaderboardForTrack(trackUid: String): List<Recording> =
        recordingDao.getLeaderboardForTrack(trackUid).map { it.toDomain() }

    override suspend fun getBestForTrack(trackUid: String): Recording? =
        recordingDao.getBestForTrack(trackUid)?.toDomain()

    override suspend fun getProfileStats(): ProfileStats {
        val total = recordingDao.countAll()
        val completed = recordingDao.countCompleted()
        val distance = recordingDao.sumAllDistance()
        val duration = recordingDao.sumAllDuration()
        val best = recordingDao.getBestOverall()
        return ProfileStats(
            totalRecordings = total,
            completedRecordings = completed,
            totalDistance = distance,
            totalDurationMs = duration,
            bestTimeMs = best?.let { it.endTime - it.startTime },
        )
    }

    private fun RecordingEntity.toDomain() = Recording(
        uid = uid,
        trackUid = trackUid,
        mode = mode,
        startTime = startTime,
        endTime = endTime,
        totalDistance = totalDistance,
        avgSpeed = avgSpeed,
        maxSpeed = maxSpeed,
        status = status,
        createdAt = createdAt,
    )

    private fun Recording.toEntity() = RecordingEntity(
        uid = uid,
        trackUid = trackUid,
        mode = mode,
        startTime = startTime,
        endTime = endTime,
        totalDistance = totalDistance,
        avgSpeed = avgSpeed,
        maxSpeed = maxSpeed,
        status = status,
        createdAt = createdAt,
    )
}
