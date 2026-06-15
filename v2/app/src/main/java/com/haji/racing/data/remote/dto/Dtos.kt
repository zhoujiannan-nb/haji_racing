package com.haji.racing.data.remote.dto

data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "ok",
    val data: T? = null,
)

data class LoginRequest(
    val account: String,
    val password: String,
)

data class LoginResponse(
    val token: String,
    val uid: String,
    val nickname: String,
    val avatarUrl: String? = null,
)

data class RegisterRequest(
    val account: String,
    val password: String,
    val nickname: String,
    val email: String? = null,
)

data class TrackDto(
    val uid: String,
    val name: String,
    val description: String? = null,
    val type: String,
    val startFencePoints: List<FencePointDto> = emptyList(),
    val endFencePoints: List<FencePointDto> = emptyList(),
    val totalDistance: Double,
    val creatorUid: String?,
    val creatorName: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class FencePointDto(
    val lat: Double,
    val lng: Double,
)

data class RecordingDto(
    val uid: String,
    val trackUid: String,
    val mode: String,
    val startTime: Long,
    val endTime: Long,
    val totalDistance: Double,
    val avgSpeed: Double,
    val maxSpeed: Double,
    val userUid: String,
    val createdAt: Long,
)

data class UserDto(
    val uid: String,
    val account: String? = null,
    val nickname: String,
    val avatarUrl: String?,
    val token: String? = null,
    val totalDistance: Double,
    val totalRecordings: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
