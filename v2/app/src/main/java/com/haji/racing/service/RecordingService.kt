package com.haji.racing.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.haji.racing.MainActivity
import com.haji.racing.R
import com.haji.racing.core.common.DistanceUtils
import com.haji.racing.core.common.TimeUtils
import com.haji.racing.core.fusion.FusedPosition
import com.haji.racing.core.fusion.PositionFusion
import com.haji.racing.core.geofence.GeofenceDetector
import com.haji.racing.core.gps.GpsPoint
import com.haji.racing.core.gps.GpsTracker
import com.haji.racing.core.sensor.AccelerometerManager
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
    val currentG: Float = 0f,
    val maxG: Float = 0f,
    val gSum: Float = 0f,
    val gCount: Int = 0,
    val timeDiffMs: Long = 0, // vs reference track
    val recordingUid: String? = null,
)

@AndroidEntryPoint
class RecordingService : LifecycleService() {

    @Inject lateinit var gpsTracker: GpsTracker
    @Inject lateinit var accelerometerManager: AccelerometerManager
    @Inject lateinit var recordingDao: RecordingDao
    @Inject lateinit var recordingPointDao: RecordingPointDao

    companion object {
        const val CHANNEL_ID = "haji_recording_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_TRACK_UID = "track_uid"
        const val EXTRA_MODE = "mode"
        const val EXTRA_REF_RECORDING_UID = "ref_recording_uid"

        private val _recordingData = MutableStateFlow(RecordingData())
        val recordingData: StateFlow<RecordingData> = _recordingData
    }

    private var positionFusion = PositionFusion()
    private var track: Track? = null
    private var mode: String = "track"
    private var refRecordingUid: String? = null
    private var recordingUid: String = ""
    private var startTime: Long = 0
    private var lastGpsPoint: GpsPoint? = null
    private var totalDistance: Double = 0.0
    private var maxSpeed: Double = 0.0
    private var maxG: Float = 0f
    private var gSum: Float = 0f
    private var gCount: Int = 0
    private var isInitialized = false
    private var passedStart = false

    private var refDistances: List<Double>? = null
    private var refTimestamps: List<Long>? = null

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
        refRecordingUid = intent.getStringExtra(EXTRA_REF_RECORDING_UID)
        recordingUid = UUID.randomUUID().toString()
        startTime = System.currentTimeMillis()
        totalDistance = 0.0
        maxSpeed = 0.0
        maxG = 0f
        gSum = 0f
        gCount = 0
        passedStart = false
        isInitialized = false

        positionFusion.reset()

        _recordingData.value = RecordingData(state = RecordingState.WAITING, recordingUid = recordingUid)

        startForeground(NOTIFICATION_ID, createNotification("等待起跑..."))

        loadReferenceRecording()

        lifecycleScope.launch {
            gpsTracker.startTracking()
            accelerometerManager.startListening()
        }

        lifecycleScope.launch(Dispatchers.Default) {
            gpsTracker.gpsFlow.collect { gpsPoint ->
                handleGpsUpdate(gpsPoint)
            }
        }

        lifecycleScope.launch(Dispatchers.Default) {
            accelerometerManager.accelFlow.collect { accelData ->
                handleAccelUpdate(
                    floatArrayOf(accelData.x, accelData.y, accelData.z),
                    accelData.timestamp
                )
            }
        }
    }

    private fun loadReferenceRecording() {
        if (refRecordingUid != null) {
            lifecycleScope.launch {
                val points = recordingPointDao.getPointsForRecordingSync(refRecordingUid!!)
                if (points.isNotEmpty()) {
                    refDistances = points.map { it.distance }
                    refTimestamps = points.map { it.timestamp - points.first().timestamp }
                }
            }
        }
    }

    private suspend fun handleGpsUpdate(gpsPoint: GpsPoint) {
        if (!isInitialized) {
            positionFusion.init(gpsPoint)
            isInitialized = true
        }

        val fused = positionFusion.updateWithGps(gpsPoint)
        lastGpsPoint = gpsPoint

        when (_recordingData.value.state) {
            RecordingState.WAITING -> {
                val shouldStart = if (mode == "track" && track != null) {
                    GeofenceDetector.isInsidePolygon(
                        fused.lat, fused.lng,
                        track!!.startFencePoints
                    ) && gpsPoint.speed >= 4.17 // 15 km/h in m/s
                } else {
                    gpsPoint.speed >= 2.0 // any speed for cruise mode
                }

                if (shouldStart) {
                    passedStart = true
                    startTime = System.currentTimeMillis()
                    _recordingData.value = _recordingData.value.copy(state = RecordingState.RECORDING)
                }
            }
            RecordingState.RECORDING -> {
                updateRecordingState(fused, gpsPoint)

                if (mode == "track" && track != null) {
                    val atEnd = GeofenceDetector.isInsidePolygon(
                        fused.lat, fused.lng,
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

    private fun handleAccelUpdate(rawAccel: FloatArray, timestamp: Long) {
        if (_recordingData.value.state != RecordingState.RECORDING) return

        val gValue = accelerometerManager.getGValue(rawAccel[0], rawAccel[1], rawAccel[2])
        val fused = positionFusion.updateWithAccel(rawAccel, gValue, timestamp)

        if (gValue > maxG) maxG = gValue
        gSum += gValue
        gCount++

        val point = RecordingPointEntity(
            recordingUid = recordingUid,
            timestamp = timestamp,
            lat = fused.lat,
            lng = fused.lng,
            speed = fused.speed,
            accelerationX = fused.acceleration[0],
            accelerationY = fused.acceleration[1],
            accelerationZ = fused.acceleration[2],
            gValue = gValue,
            distance = totalDistance,
        )

        lifecycleScope.launch { recordingPointDao.insertPoint(point) }

        val timeDiff = calculateTimeDiff(totalDistance, timestamp - startTime)

        _recordingData.value = _recordingData.value.copy(
            currentG = gValue,
            maxG = maxG,
            gSum = gSum,
            gCount = gCount,
            timeDiffMs = timeDiff,
        )
    }

    private fun updateRecordingState(fused: FusedPosition, gpsPoint: GpsPoint) {
        val prev = lastGpsPoint
        if (prev != null) {
            totalDistance += DistanceUtils.haversine(prev.lat, prev.lng, gpsPoint.lat, gpsPoint.lng)
        }

        if (gpsPoint.speed > maxSpeed) maxSpeed = gpsPoint.speed

        val elapsed = System.currentTimeMillis() - startTime
        val timeDiff = calculateTimeDiff(totalDistance, elapsed)

        _recordingData.value = _recordingData.value.copy(
            elapsedMs = elapsed,
            currentDistance = totalDistance,
            currentSpeed = fused.speed,
            maxSpeed = maxSpeed,
            timeDiffMs = timeDiff,
        )
    }

    private fun calculateTimeDiff(currentDist: Double, elapsed: Long): Long {
        val refDist = refDistances ?: return 0
        val refTs = refTimestamps ?: return 0
        if (refDist.isEmpty()) return 0

        val refIdx = refDist.indexOfFirst { it >= currentDist }
        if (refIdx < 0) return 0

        return refTs[refIdx] - elapsed
    }

    private fun finishRecording() {
        _recordingData.value = _recordingData.value.copy(state = RecordingState.FINISHED)
        stopSensors()
        saveRecording()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopRecording() {
        _recordingData.value = _recordingData.value.copy(state = RecordingState.FINISHED)
        stopSensors()
        if (_recordingData.value.state == RecordingState.RECORDING) {
            saveRecording()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopSensors() {
        gpsTracker.stopTracking()
        accelerometerManager.stopListening()
    }

    private fun saveRecording() {
        val elapsed = _recordingData.value.elapsedMs
        val dist = _recordingData.value.currentDistance
        val avgSpeed = if (elapsed > 0) dist / (elapsed / 1000.0) else 0.0
        val avgG = if (gCount > 0) gSum / gCount else 0f

        val entity = RecordingEntity(
            uid = recordingUid,
            trackUid = track?.uid ?: "",
            mode = mode,
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            totalDistance = dist,
            avgSpeed = avgSpeed,
            maxSpeed = maxSpeed,
            avgG = avgG.toDouble(),
            maxG = maxG.toDouble(),
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
