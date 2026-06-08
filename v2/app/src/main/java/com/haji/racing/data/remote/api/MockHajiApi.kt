package com.haji.racing.data.remote.api

import com.haji.racing.data.remote.dto.ApiResponse
import com.haji.racing.data.remote.dto.RecordingDto
import com.haji.racing.data.remote.dto.TrackDto
import com.haji.racing.data.remote.dto.UserDto

class MockHajiApi : HajiApi {
    override suspend fun getOfficialTracks(): ApiResponse<List<TrackDto>> =
        ApiResponse(data = emptyList())

    override suspend fun uploadTrack(track: TrackDto): ApiResponse<TrackDto> =
        ApiResponse(data = track)

    override suspend fun getRecordings(): ApiResponse<List<RecordingDto>> =
        ApiResponse(data = emptyList())

    override suspend fun uploadRecording(recording: RecordingDto): ApiResponse<RecordingDto> =
        ApiResponse(data = recording)

    override suspend fun login(credentials: Map<String, String>): ApiResponse<UserDto> =
        ApiResponse(data = UserDto(
            uid = "mock-user-001",
            nickname = "测试用户",
            avatarUrl = null,
            totalDistance = 0.0,
            totalRecordings = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        ))

    override suspend fun getProfile(): ApiResponse<UserDto> =
        ApiResponse(data = UserDto(
            uid = "mock-user-001",
            nickname = "测试用户",
            avatarUrl = null,
            totalDistance = 0.0,
            totalRecordings = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        ))

    override suspend fun updateProfile(user: UserDto): ApiResponse<UserDto> =
        ApiResponse(data = user)
}
