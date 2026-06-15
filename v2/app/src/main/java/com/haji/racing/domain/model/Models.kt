package com.haji.racing.domain.model

data class Track(
    val uid: String,
    val name: String,
    val description: String? = null,
    val type: String,
    val startFencePoints: List<FencePoint> = emptyList(),
    val endFencePoints: List<FencePoint> = emptyList(),
    val totalDistance: Double,
    val creatorUid: String?,
    val creatorName: String? = null,
    val isSynced: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class FencePoint(
    val lat: Double,
    val lng: Double,
)

data class Recording(
    val uid: String,
    val trackUid: String,
    val mode: String,
    val startTime: Long,
    val endTime: Long,
    val totalDistance: Double,
    val avgSpeed: Double,
    val maxSpeed: Double,
    val userUid: String,
    val isSynced: Boolean,
    val createdAt: Long,
    val points: List<RecordingPoint> = emptyList(),
)

data class RecordingPoint(
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val speed: Double,
    val distance: Double,
)

data class User(
    val uid: String,
    val account: String? = null,
    val nickname: String,
    val avatarUrl: String?,
    val token: String? = null,
    val totalDistance: Double,
    val totalRecordings: Int,
    val isSynced: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
