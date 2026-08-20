package com.haji.racing.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_points")
data class RecordingPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingUid: String,
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val speed: Double, // m/s
    val distance: Double, // cumulative distance in meters
)
