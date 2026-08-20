package com.haji.racing.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.haji.racing.MainActivity
import com.haji.racing.R
import com.haji.racing.core.common.TimeUtils
import com.haji.racing.core.geo.GeoMath
import com.haji.racing.core.gps.GpsPoint
import com.haji.racing.core.gps.GpsTracker
import com.haji.racing.data.local.db.dao.RecordingDao
import com.haji.racing.data.local.db.dao.RecordingPointDao
import com.haji.racing.data.local.db.dao.TrackDao
import com.haji.racing.data.local.db.entity.RecordingEntity
import com.haji.racing.data.local.db.entity.RecordingPointEntity
import com.haji.racing.data.local.db.entity.TrackEntity
import com.haji.racing.domain.model.FencePoint
import com.haji.racing.domain.model.MODE_FREE
import com.haji.racing.domain.model.MODE_TRACK
import com.haji.racing.domain.model.STATUS_COMPLETED
import com.haji.racing.domain.model.STATUS_INCOMPLETE
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class RecordingState {
    IDLE, WAITING, RECORDING, FINISHED,
}

/** 服务侧的实时状态（供 UI 收集） */
data class RecordingData(
    val state: RecordingState = RecordingState.IDLE,
    val trackUid: String? = null,
    val mode: String = MODE_TRACK,
    val recordingUid: String? = null,
    val timingStartEpoch: Long = 0L,
    val elapsedMs: Long = 0L,
    val currentDistance: Double = 0.0,
    val currentSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val accuracy: Float = Float.MAX_VALUE,
    val inStartZone: Boolean = false,
    val inEndZone: Boolean = false,
    val distanceToStart: Double? = null,
    val gpsReady: Boolean = false,
)

/** 一次记录完成后的结果 */
data class RecordingResult(
    val uid: String,
    val trackUid: String,
    val mode: String,
    val status: String,
    val durationMs: Long,
    val totalDistance: Double,
    val avgSpeed: Double,
    val maxSpeed: Double,
)

/**
 * 前台记录服务：
 * - 赛道模式: WAITING（起点围栏 + >=15km/h 触发计时）-> RECORDING -> 进入终点围栏自动完成
 * - 自由模式: RECORDING -> 手动停止（completed）
 * - 手动停止（赛道模式）保存为 incomplete
 * - 未触发计时就停止：不保存
 */
@AndroidEntryPoint
class RecordingService : LifecycleService() {

    @Inject lateinit var gpsTracker: GpsTracker
    @Inject lateinit var trackDao: TrackDao
    @Inject lateinit var recordingDao: RecordingDao
    @Inject lateinit var pointDao: RecordingPointDao
    @Inject lateinit var gson: Gson

    companion object {
        const val CHANNEL_ID = "haji_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.haji.racing.action.START"
        const val ACTION_STOP = "com.haji.racing.action.STOP"
        const val EXTRA_TRACK_UID = "track_uid"
        const val EXTRA_MODE = "mode"

        /** 起点触发速度（km/h） */
        const val START_TRIGGER_SPEED_KMH = 15.0

        private const val MAX_POINT_ACCURACY = 50f
        private const val MAX_JUMP_METERS = 200.0
        private const val POINT_INTERVAL_MS = 1000L

        private val _recordingData = MutableStateFlow(RecordingData())
        val recordingData: StateFlow<RecordingData> = _recordingData.asStateFlow()

        private val _liveRoute = MutableStateFlow<List<GpsPoint>>(emptyList())
        val liveRoute: StateFlow<List<GpsPoint>> = _liveRoute.asStateFlow()

        private val _result = MutableStateFlow<RecordingResult?>(null)
        val result: StateFlow<RecordingResult?> = _result.asStateFlow()
    }

    private var track: TrackEntity? = null
    private var trackLoaded = false
    private var startFence: List<FencePoint> = emptyList()
    private var endFence: List<FencePoint> = emptyList()
    private var mode = MODE_TRACK
    private var recordingUid = ""
    private var timingStart = 0L
    private var timed = false
    private var lastGps: GpsPoint? = null
    private var totalDistance = 0.0
    private var maxSpeed = 0.0
    private var lastPointRecordTime = 0L
    private var finishing = false
    private val pointBuffer = mutableListOf<RecordingPointEntity>()
    private var wakeLock: PowerManager.WakeLock? = null
    private var gpsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_STOP -> requestStop()
        }
        return START_NOT_STICKY
    }

    // ------------------------------------------------------------------
    // 启动 / 停止
    // ------------------------------------------------------------------

    private fun startRecording(intent: Intent) {
        if (_recordingData.value.state != RecordingState.IDLE) return

        _result.value = null
        _liveRoute.value = emptyList()
        pointBuffer.clear()
        totalDistance = 0.0
        maxSpeed = 0.0
        timed = false
        lastGps = null
        finishing = false
        recordingUid = UUID.randomUUID().toString()
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_TRACK
        val trackUid = intent.getStringExtra(EXTRA_TRACK_UID) ?: ""

        if (mode == MODE_TRACK && trackUid.isNotEmpty()) {
            _recordingData.value = RecordingData(
                state = RecordingState.WAITING,
                trackUid = trackUid,
                mode = mode,
                recordingUid = recordingUid,
            )
            lifecycleScope.launch {
                val entity = trackDao.getTrackByUid(trackUid)
                track = entity
                startFence = entity?.let { parseFencePoints(it.startFencePointsJson) } ?: emptyList()
                endFence = entity?.let { parseFencePoints(it.endFencePointsJson) } ?: emptyList()
                trackLoaded = true
                updateData { it.copy(inStartZone = false, inEndZone = false) }
            }
        } else {
            // 自由模式（或赛道不存在时降级为自由）
            track = null
            trackLoaded = true
            startTiming()
        }

        acquireWakeLock()
        startForeground(NOTIFICATION_ID, buildNotification())
        gpsTracker.startTracking()
        gpsJob = lifecycleScope.launch {
            gpsTracker.gpsFlow.collect { handleGps(it) }
        }
    }

    private fun requestStop() {
        when (_recordingData.value.state) {
            RecordingState.WAITING -> finishRecording(completed = false)
            RecordingState.RECORDING -> finishRecording(completed = mode == MODE_FREE)
            else -> {
                // 已经在结束流程或空闲：清理
                gpsTracker.stopTracking()
                releaseWakeLock()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun startTiming() {
        timed = true
        timingStart = System.currentTimeMillis()
        lastPointRecordTime = timingStart - POINT_INTERVAL_MS
        _recordingData.value = _recordingData.value.copy(
            state = RecordingState.RECORDING,
            timingStartEpoch = timingStart,
        )
        updateNotification()
    }

    private fun finishRecording(completed: Boolean) {
        if (finishing) return
        finishing = true

        val endTime = System.currentTimeMillis()
        if (timed) {
            val elapsed = (endTime - timingStart).coerceAtLeast(0L)
            val avg = if (elapsed > 0) totalDistance / (elapsed / 1000.0) else 0.0
            val status = if (completed) STATUS_COMPLETED else STATUS_INCOMPLETE
            val entity = RecordingEntity(
                uid = recordingUid,
                trackUid = track?.uid ?: "",
                mode = mode,
                startTime = timingStart,
                endTime = endTime,
                totalDistance = totalDistance,
                avgSpeed = avg,
                maxSpeed = maxSpeed,
                status = status,
                createdAt = endTime,
            )
            val result = RecordingResult(
                uid = recordingUid,
                trackUid = track?.uid ?: "",
                mode = mode,
                status = status,
                durationMs = elapsed,
                totalDistance = totalDistance,
                avgSpeed = avg,
                maxSpeed = maxSpeed,
            )
            updateData {
                it.copy(
                    state = RecordingState.FINISHED,
                    elapsedMs = elapsed,
                    currentDistance = totalDistance,
                    maxSpeed = maxSpeed,
                )
            }
            _result.value = result
            updateNotification()
            lifecycleScope.launch {
                try {
                    flushPoints()
                    recordingDao.insertRecording(entity)
                } finally {
                    shutdown()
                }
            }
        } else {
            // 未触发计时：不保存
            updateData { it.copy(state = RecordingState.FINISHED) }
            _result.value = null
            shutdown()
        }
    }

    private fun shutdown() {
        gpsJob?.cancel()
        gpsTracker.stopTracking()
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ------------------------------------------------------------------
    // GPS 处理
    // ------------------------------------------------------------------

    private fun handleGps(p: GpsPoint) {
        when (_recordingData.value.state) {
            RecordingState.WAITING -> handleWaiting(p)
            RecordingState.RECORDING -> handleRecording(p)
            else -> Unit
        }
    }

    private fun handleWaiting(p: GpsPoint) {
        if (!trackLoaded) {
            updateData { it.copy(gpsReady = true, accuracy = p.accuracy) }
            return
        }
        val inStart = startFence.isNotEmpty() && GeoMath.isPointInPolygon(p.lat, p.lng, startFence)
        val dist = if (startFence.isNotEmpty()) GeoMath.distanceToPolygon(p.lat, p.lng, startFence) else null
        val readyToGo = inStart && p.speed >= START_TRIGGER_SPEED_KMH / 3.6
        updateData {
            it.copy(
                gpsReady = true,
                accuracy = p.accuracy,
                inStartZone = inStart,
                distanceToStart = dist,
                currentSpeed = p.speed,
            )
        }
        if (readyToGo) startTiming()
    }

    private fun handleRecording(p: GpsPoint) {
        val prev = lastGps
        lastGps = p

        // 累计距离（过滤精度差与 GPS 跳变）
        if (prev != null && p.accuracy <= MAX_POINT_ACCURACY) {
            val d = GeoMath.distance(prev.lat, prev.lng, p.lat, p.lng)
            if (d <= MAX_JUMP_METERS) totalDistance += d
        }
        if (p.speed > maxSpeed) maxSpeed = p.speed

        val inEnd = mode == MODE_TRACK && endFence.isNotEmpty() &&
            GeoMath.isPointInPolygon(p.lat, p.lng, endFence)

        recordPointIfNeeded(p)

        val now = System.currentTimeMillis()
        updateData {
            it.copy(
                gpsReady = true,
                accuracy = p.accuracy,
                elapsedMs = (now - timingStart).coerceAtLeast(0L),
                currentDistance = totalDistance,
                currentSpeed = p.speed,
                maxSpeed = maxSpeed,
                inEndZone = inEnd,
            )
        }

        if (inEnd) finishRecording(completed = true)
    }

    private fun recordPointIfNeeded(p: GpsPoint) {
        val now = System.currentTimeMillis()
        if (now - lastPointRecordTime < POINT_INTERVAL_MS) return
        lastPointRecordTime = now
        pointBuffer.add(
            RecordingPointEntity(
                recordingUid = recordingUid,
                timestamp = now,
                lat = p.lat,
                lng = p.lng,
                speed = p.speed,
                distance = totalDistance,
            ),
        )
        _liveRoute.value = _liveRoute.value + p
        if (pointBuffer.size >= 10) flushPointsNow()
        updateNotification()
    }

    private fun flushPointsNow() {
        if (pointBuffer.isEmpty()) return
        val batch = pointBuffer.toList()
        pointBuffer.clear()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                pointDao.insertPoints(batch)
            } catch (e: Exception) {
                android.util.Log.e("RecordingService", "insertPoints failed", e)
            }
        }
    }

    private suspend fun flushPoints() {
        if (pointBuffer.isEmpty()) return
        val batch = pointBuffer.toList()
        pointBuffer.clear()
        try {
            pointDao.insertPoints(batch)
        } catch (e: Exception) {
            android.util.Log.e("RecordingService", "flushPoints failed", e)
        }
    }

    private inline fun updateData(transform: (RecordingData) -> RecordingData) {
        _recordingData.value = transform(_recordingData.value)
    }

    private fun parseFencePoints(json: String): List<FencePoint> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val type = object : TypeToken<List<FencePoint>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ------------------------------------------------------------------
    // 通知 & 锁
    // ------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (getSystemService(Context.NOTIFICATION_SERVICE) !is NotificationManager) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "赛道记录",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "跟跑/自由跑记录进行中的通知" }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val (title, text) = currentNotificationText()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun currentNotificationText(): Pair<String, String> {
        val data = _recordingData.value
        val title = if (data.mode == MODE_TRACK) "Haji Racing · 跟跑" else "Haji Racing · 自由跑"
        val text = when (data.state) {
            RecordingState.WAITING -> "等待起跑：进入起点区域并加速至 ${START_TRIGGER_SPEED_KMH.toInt()} km/h"
            RecordingState.RECORDING ->
                "${TimeUtils.formatDurationPrecise(data.elapsedMs)} · " +
                    "${formatKm(data.currentDistance)} · ${formatKmh(data.currentSpeed)}"
            RecordingState.FINISHED ->
                "已完成 ${TimeUtils.formatDurationPrecise(data.elapsedMs)} · " +
                    "${formatKm(data.currentDistance)}"
            else -> "准备中…"
        }
        return title to text
    }

    private fun updateNotification() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            // 权限未授予时忽略
        }
    }

    private fun formatKm(meters: Double): String =
        if (meters >= 1000) "%.2f km".format(meters / 1000) else "%.0f m".format(meters)

    private fun formatKmh(mps: Double): String = "%.0f km/h".format(mps * 3.6)

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "haji:racing:recording")
                .apply { acquire(4 * 60 * 60 * 1000L) }
        } catch (e: Exception) {
            android.util.Log.e("RecordingService", "acquireWakeLock failed", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    override fun onDestroy() {
        gpsJob?.cancel()
        gpsTracker.stopTracking()
        releaseWakeLock()
        super.onDestroy()
    }
}
