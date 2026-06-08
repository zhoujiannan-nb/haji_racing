package com.haji.racing.domain.model

data class Track(
    val uid: String,
    val name: String,
    val type: String,
    val startLat: Double,
    val startLng: Double,
    val startFenceRadius: Double,
    val endLat: Double,
    val endLng: Double,
    val endFenceRadius: Double,
    val totalDistance: Double,
    val creatorUid: String?,
    val isSynced: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val points: List<TrackPoint> = emptyList(),
)

data class TrackPoint(
    val lat: Double,
    val lng: Double,
    val sequenceIndex: Int,
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
    val avgG: Double,
    val maxG: Double,
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
    val accelerationX: Float,
    val accelerationY: Float,
    val accelerationZ: Float,
    val gValue: Float,
    val distance: Double,
)

data class User(
    val uid: String,
    val nickname: String,
    val avatarUrl: String?,
    val totalDistance: Double,
    val totalRecordings: Int,
    val isSynced: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
