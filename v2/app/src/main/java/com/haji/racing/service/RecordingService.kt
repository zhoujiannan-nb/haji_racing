package com.haji.racing.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.haji.racing.MainActivity
import com.haji.racing.R
import com.haji.racing.core.common.DistanceUtils
import com.haji.racing.core.geofence.GeofenceDetector
import com.haji.racing.core.gps.GpsPoint
import com.haji.racing.core.gps.GpsTracker
import com.haji.racing.data.local.db.dao.RecordingDao
import com.haji.racing.data.local.db.dao.RecordingPointDao
import com.haji.racing.data.local.db.entity.RecordingEntity
import com.haji.racing.data.local.db.entity.RecordingPointEntity
import com.haji.racing.domain.model.Track
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class RecordingState {
    IDLE, WAITING, RECORDING, FINISHED
}

data class RecordingData(
    val state: RecordingState = RecordingState.IDLE,
    val elapsedMs: Long = 0,
    val currentDistance: Double = 0.0,
    val currentSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val recordingUid: String? = null,
)

@AndroidEntryPoint
class RecordingService : LifecycleService() {

    @Inject lateinit var gpsTracker: GpsTracker
    @Inject lateinit var recordingDao: RecordingDao
    @Inject lateinit var recordingPointDao: RecordingPointDao

    companion object {
        const val CHANNEL_ID = "haji_recording_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_TRACK_UID = "track_uid"
        const val EXTRA_MODE = "mode"

        private val _recordingData = MutableStateFlow(RecordingData())
        val recordingData: StateFlow<RecordingData> = _recordingData
    }

    private var track: Track? = null
    private var mode: String = "track"
    private var recordingUid: String = ""
    private var startTime: Long = 0
    private var lastGpsPoint: GpsPoint? = null
    private var totalDistance: Double = 0.0
    private var maxSpeed: Double = 0.0
    private var passedStart = false
    private var lastRecordTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_STOP -> stopRecording()
        }

        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        val trackUid = intent.getStringExtra(EXTRA_TRACK_UID) ?: return
        mode = intent.getStringExtra(EXTRA_MODE) ?: "track"
        recordingUid = UUID.randomUUID().toString()
        startTime = System.currentTimeMillis()
        totalDistance = 0.0
        maxSpeed = 0.0
        passedStart = false
        lastRecordTime = 0

        _recordingData.value = RecordingData(state = RecordingState.WAITING, recordingUid = recordingUid)

        startForeground(NOTIFICATION_ID, createNotification("等待起跑..."))

        lifecycleScope.launch {
            gpsTracker.startTracking()
        }

        lifecycleScope.launch(Dispatchers.Default) {
            gpsTracker.gpsFlow.collect { gpsPoint ->
                handleGpsUpdate(gpsPoint)
            }
        }
    }

    private suspend fun handleGpsUpdate(gpsPoint: GpsPoint) {
        lastGpsPoint = gpsPoint

        when (_recordingData.value.state) {
            RecordingState.WAITING -> {
                val shouldStart = if (mode == "track" && track != null) {
                    GeofenceDetector.isInsidePolygon(
                        gpsPoint.lat, gpsPoint.lng,
                        track!!.startFencePoints
                    ) && gpsPoint.speed >= 4.17 // 15 km/h in m/s
                } else {
                    true // free mode starts immediately
                }

                if (shouldStart) {
                    passedStart = true
                    startTime = System.currentTimeMillis()
                    lastRecordTime = startTime
                    _recordingData.value = _recordingData.value.copy(state = RecordingState.RECORDING)
                }
            }
            RecordingState.RECORDING -> {
                updateRecordingState(gpsPoint)
                recordPointIfNeeded(gpsPoint)

                if (mode == "track" && track != null) {
                    val atEnd = GeofenceDetector.isInsidePolygon(
                        gpsPoint.lat, gpsPoint.lng,
                        track!!.endFencePoints
                    )
                    if (atEnd && passedStart) {
                        finishRecording()
                    }
                }
            }
            else -> {}
        }
    }

    private fun recordPointIfNeeded(gpsPoint: GpsPoint) {
        val now = System.currentTimeMillis()
        val interval = if (mode == "free") 1000L else 100L // 1s for free, 100ms for track

        if (now - lastRecordTime >= interval) {
            lastRecordTime = now
            val point = RecordingPointEntity(
                recordingUid = recordingUid,
                timestamp = now,
                lat = gpsPoint.lat,
                lng = gpsPoint.lng,
                speed = gpsPoint.speed,
                distance = totalDistance,
            )
            lifecycleScope.launch { recordingPointDao.insertPoint(point) }
        }
    }

    private fun updateRecordingState(gpsPoint: GpsPoint) {
        val prev = lastGpsPoint
        if (prev != null) {
            totalDistance += DistanceUtils.haversine(prev.lat, prev.lng, gpsPoint.lat, gpsPoint.lng)
        }

        if (gpsPoint.speed > maxSpeed) maxSpeed = gpsPoint.speed

        val elapsed = System.currentTimeMillis() - startTime

        _recordingData.value = _recordingData.value.copy(
            elapsedMs = elapsed,
            currentDistance = totalDistance,
            currentSpeed = gpsPoint.speed,
            maxSpeed = maxSpeed,
        )
    }

    private fun finishRecording() {
        _recordingData.value = _recordingData.value.copy(state = RecordingState.FINISHED)
        stopSensors()
        saveRecording()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopRecording() {
        val wasRecording = _recordingData.value.state == RecordingState.RECORDING
        _recordingData.value = _recordingData.value.copy(state = RecordingState.FINISHED)
        stopSensors()
        if (wasRecording) {
            saveRecording()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopSensors() {
        gpsTracker.stopTracking()
    }

    private fun saveRecording() {
        val elapsed = _recordingData.value.elapsedMs
        val dist = _recordingData.value.currentDistance
        val avgSpeed = if (elapsed > 0) dist / (elapsed / 1000.0) else 0.0

        val entity = RecordingEntity(
            uid = recordingUid,
            trackUid = track?.uid ?: "",
            mode = mode,
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            totalDistance = dist,
            avgSpeed = avgSpeed,
            maxSpeed = maxSpeed,
            userUid = "",
            createdAt = System.currentTimeMillis(),
        )

        lifecycleScope.launch { recordingDao.insertRecording(entity) }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "赛道记录",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "赛道记录进行中的通知" }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Haji Racing")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensors()
    }
}
