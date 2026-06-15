package com.haji.racing.data.remote.api

import com.haji.racing.data.remote.dto.ApiResponse
import com.haji.racing.data.remote.dto.TrackDto
import com.haji.racing.data.remote.dto.UserDto
import com.haji.racing.data.remote.dto.RecordingDto
import com.haji.racing.data.remote.dto.LoginRequest
import com.haji.racing.data.remote.dto.LoginResponse
import com.haji.racing.data.remote.dto.RegisterRequest
import retrofit2.http.*

interface HajiApi {
    @POST("api/auth/login")
    suspend fun login(@Body credentials: LoginRequest): ApiResponse<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<Unit>

    @GET("api/v2/tracks")
    suspend fun getOfficialTracks(): ApiResponse<List<TrackDto>>

    @POST("api/v2/tracks/sync")
    suspend fun uploadTrack(@Body track: TrackDto): ApiResponse<TrackDto>

    @GET("api/v2/recordings")
    suspend fun getRecordings(): ApiResponse<List<RecordingDto>>

    @POST("api/v2/recordings/sync")
    suspend fun uploadRecording(@Body recording: RecordingDto): ApiResponse<RecordingDto>

    @GET("users/me")
    suspend fun getProfile(): ApiResponse<UserDto>

    @PUT("users/me")
    suspend fun updateProfile(@Body user: UserDto): ApiResponse<UserDto>
}
