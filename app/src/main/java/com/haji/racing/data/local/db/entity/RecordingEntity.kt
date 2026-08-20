package com.haji.racing.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val uid: String,
    val trackUid: String, // "" = free ride
    val mode: String,     // "track" or "free"
    val startTime: Long,  // 计时开始（毫秒时间戳）
    val endTime: Long,    // 计时结束（毫秒时间戳）
    val totalDistance: Double, // meters
    val avgSpeed: Double, // m/s
    val maxSpeed: Double, // m/s
    val status: String,   // "completed" | "incomplete"
    val createdAt: Long,
)
