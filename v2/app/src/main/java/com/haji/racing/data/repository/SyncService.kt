package com.haji.racing.data.repository

import com.haji.racing.data.local.db.dao.RecordingDao
import com.haji.racing.data.local.db.dao.TrackDao
import com.haji.racing.data.remote.api.HajiApi
import com.haji.racing.data.remote.dto.FencePointDto
import com.haji.racing.data.remote.dto.RecordingDto
import com.haji.racing.data.remote.dto.TrackDto
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncService @Inject constructor(
    private val api: HajiApi,
    private val trackDao: TrackDao,
    private val recordingDao: RecordingDao,
) {
    suspend fun syncAll(): SyncResult {
        var tracksUploaded = 0
        var recordingsUploaded = 0
        var tracksDownloaded = 0
        var recordingsDownloaded = 0

        // 上传未同步的赛道
        val unsyncedTracks = trackDao.getUnsyncedTracks()
        for (track in unsyncedTracks) {
            try {
                val dto = track.toDto()
                api.uploadTrack(dto)
                trackDao.updateTrack(track.copy(isSynced = true))
                tracksUploaded++
            } catch (_: Exception) {}
        }

        // 上传未同步的录制
        val unsyncedRecordings = recordingDao.getUnsyncedRecordings()
        for (recording in unsyncedRecordings) {
            try {
                val dto = recording.toDto()
                api.uploadRecording(dto)
                recordingDao.updateRecording(recording.copy(isSynced = true))
                recordingsUploaded++
            } catch (_: Exception) {}
        }

        // 下载官方赛道
        try {
            val remoteTracks = api.getOfficialTracks()
            remoteTracks.data?.let { trackDtos ->
                for (dto in trackDtos) {
                    val entity = dto.toTrackEntity()
                    trackDao.insertTrack(entity)
                    tracksDownloaded++
                }
            }
        } catch (_: Exception) {}

        // 下载录制记录
        try {
            val remoteRecordings = api.getRecordings()
            remoteRecordings.data?.let { recDtos ->
                for (dto in recDtos) {
                    val entity = dto.toRecordingEntity()
                    recordingDao.insertRecording(entity)
                    recordingsDownloaded++
                }
            }
        } catch (_: Exception) {}

        return SyncResult(
            tracksUploaded = tracksUploaded,
            recordingsUploaded = recordingsUploaded,
            tracksDownloaded = tracksDownloaded,
            recordingsDownloaded = recordingsDownloaded,
        )
    }

    private fun com.haji.racing.data.local.db.entity.TrackEntity.toDto() = TrackDto(
        uid = uid,
        name = name,
        description = description,
        type = type,
        startFencePoints = deserializeFencePoints(startFencePointsJson).map { FencePointDto(it.lat, it.lng) },
        endFencePoints = deserializeFencePoints(endFencePointsJson).map { FencePointDto(it.lat, it.lng) },
        totalDistance = totalDistance,
        creatorUid = creatorUid,
        creatorName = creatorName,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun TrackDto.toTrackEntity() = com.haji.racing.data.local.db.entity.TrackEntity(
        uid = uid,
        name = name,
        description = description,
        type = type,
        startFencePointsJson = serializeFencePoints(startFencePoints.map { com.haji.racing.domain.model.FencePoint(it.lat, it.lng) }),
        endFencePointsJson = serializeFencePoints(endFencePoints.map { com.haji.racing.domain.model.FencePoint(it.lat, it.lng) }),
        totalDistance = totalDistance,
        creatorUid = creatorUid,
        creatorName = creatorName,
        isSynced = true,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun com.haji.racing.data.local.db.entity.RecordingEntity.toDto() = RecordingDto(
        uid = uid,
        trackUid = trackUid,
        mode = mode,
        startTime = startTime,
        endTime = endTime,
        totalDistance = totalDistance,
        avgSpeed = avgSpeed,
        maxSpeed = maxSpeed,
        userUid = userUid,
        createdAt = createdAt,
    )

    private fun RecordingDto.toRecordingEntity() = com.haji.racing.data.local.db.entity.RecordingEntity(
        uid = uid,
        trackUid = trackUid,
        mode = mode,
        startTime = startTime,
        endTime = endTime,
        totalDistance = totalDistance,
        avgSpeed = avgSpeed,
        maxSpeed = maxSpeed,
        userUid = userUid,
        isSynced = true,
        createdAt = createdAt,
    )

    private fun serializeFencePoints(points: List<com.haji.racing.domain.model.FencePoint>): String {
        return try {
            com.google.gson.Gson().toJson(points)
        } catch (_: Exception) {
            "[]"
        }
    }

    private fun deserializeFencePoints(json: String): List<com.haji.racing.domain.model.FencePoint> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<com.haji.racing.domain.model.FencePoint>>() {}.type
            com.google.gson.Gson().fromJson(json, type)
        } catch (_: Exception) {
            emptyList()
        }
    }
}

data class SyncResult(
    val tracksUploaded: Int = 0,
    val recordingsUploaded: Int = 0,
    val tracksDownloaded: Int = 0,
    val recordingsDownloaded: Int = 0,
)
