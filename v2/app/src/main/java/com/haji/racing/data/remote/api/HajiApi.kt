package com.haji.racing.data.remote.api

import com.haji.racing.data.remote.dto.ApiResponse
import com.haji.racing.data.remote.dto.TrackDto
import com.haji.racing.data.remote.dto.UserDto
import com.haji.racing.data.remote.dto.RecordingDto
import retrofit2.http.*

interface HajiApi {
    @GET("tracks")
    suspend fun getOfficialTracks(): ApiResponse<List<TrackDto>>

    @POST("tracks")
    suspend fun uploadTrack(@Body track: TrackDto): ApiResponse<TrackDto>

    @GET("recordings")
    suspend fun getRecordings(): ApiResponse<List<RecordingDto>>

    @POST("recordings")
    suspend fun uploadRecording(@Body recording: RecordingDto): ApiResponse<RecordingDto>

    @POST("auth/login")
    suspend fun login(@Body credentials: Map<String, String>): ApiResponse<UserDto>

    @GET("users/me")
    suspend fun getProfile(): ApiResponse<UserDto>

    @PUT("users/me")
    suspend fun updateProfile(@Body user: UserDto): ApiResponse<UserDto>
}
