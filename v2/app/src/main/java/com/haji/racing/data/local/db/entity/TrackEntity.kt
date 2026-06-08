package com.haji.racing.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val type: String, // "official" or "custom"
    val startLat: Double,
    val startLng: Double,
    val startFenceRadius: Double, // meters
    val endLat: Double,
    val endLng: Double,
    val endFenceRadius: Double, // meters
    val totalDistance: Double, // meters
    val creatorUid: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
