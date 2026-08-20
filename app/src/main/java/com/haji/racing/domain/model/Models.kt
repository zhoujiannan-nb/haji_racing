package com.haji.racing.domain.model

/**
 * 赛道（用户自定义地图）
 *
 * - startFencePoints: 起点围栏多边形（GCJ-02 坐标，高德地图显示坐标系）
 * - endFencePoints:   终点围栏多边形
 * - totalDistance:    起点围栏中心到终点围栏中心的估算距离（米）
 */
data class Track(
    val uid: String,
    val name: String,
    val description: String? = null,
    val startFencePoints: List<FencePoint> = emptyList(),
    val endFencePoints: List<FencePoint> = emptyList(),
    val totalDistance: Double,
    val creatorName: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class FencePoint(
    val lat: Double,
    val lng: Double,
)

/**
 * 一次跟跑/自由跑记录
 *
 * - trackUid: "" 表示自由跑
 * - mode: "track" | "free"
 * - status: "completed"（跑完）| "incomplete"（中途手动停止）
 * - 坐标统一为 GCJ-02（与围栏、地图一致）
 */
data class Recording(
    val uid: String,
    val trackUid: String,
    val mode: String,
    val startTime: Long,
    val endTime: Long,
    val totalDistance: Double,
    val avgSpeed: Double, // m/s
    val maxSpeed: Double, // m/s
    val status: String,
    val createdAt: Long,
    val points: List<RecordingPoint> = emptyList(),
)

data class RecordingPoint(
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val speed: Double, // m/s
    val distance: Double, // 累计米数
)

val Recording.durationMs: Long
    get() = (endTime - startTime).coerceAtLeast(0L)

val Recording.isCompleted: Boolean
    get() = status == "completed"

const val STATUS_COMPLETED = "completed"
const val STATUS_INCOMPLETE = "incomplete"
const val MODE_TRACK = "track"
const val MODE_FREE = "free"

/** 我的页聚合统计 */
data class ProfileStats(
    val totalRecordings: Int = 0,
    val completedRecordings: Int = 0,
    val totalDistance: Double = 0.0,
    val totalDurationMs: Long = 0,
    val bestTimeMs: Long? = null,
)

/** 单条赛道的统计（用于赛道卡片） */
data class TrackStats(
    val trackUid: String,
    val recordCount: Int,
    val bestTimeMs: Long?,
)
