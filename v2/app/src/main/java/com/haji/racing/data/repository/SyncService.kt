package com.haji.racing.data.repository

import com.haji.racing.data.local.db.dao.RecordingDao
import com.haji.racing.data.local.db.dao.TrackDao
import com.haji.racing.data.remote.api.HajiApi
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

        val unsyncedTracks = trackDao.getUnsyncedTracks()
        for (track in unsyncedTracks) {
            try {
                // mock upload - just mark as synced
                trackDao.updateTrack(track.copy(isSynced = true))
                tracksUploaded++
            } catch (_: Exception) {}
        }

        val unsyncedRecordings = recordingDao.getUnsyncedRecordings()
        for (recording in unsyncedRecordings) {
            try {
                recordingDao.updateRecording(recording.copy(isSynced = true))
                recordingsUploaded++
            } catch (_: Exception) {}
        }

        try {
            val remoteTracks = api.getOfficialTracks()
            remoteTracks.data?.let { trackDtos ->
                for (dto in trackDtos) {
                    tracksDownloaded++
                }
            }
        } catch (_: Exception) {}

        try {
            val remoteRecordings = api.getRecordings()
            remoteRecordings.data?.let { recDtos ->
                for (dto in recDtos) {
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
}

data class SyncResult(
    val tracksUploaded: Int = 0,
    val recordingsUploaded: Int = 0,
    val tracksDownloaded: Int = 0,
    val recordingsDownloaded: Int = 0,
)
