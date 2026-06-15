package com.haji.racing.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val uid: String,
    val trackUid: String,
    val mode: String, // "track" or "free"
    val startTime: Long,
    val endTime: Long,
    val totalDistance: Double, // meters
    val avgSpeed: Double, // m/s
    val maxSpeed: Double, // m/s
    val userUid: String,
    val isSynced: Boolean = false,
    val createdAt: Long,
)
