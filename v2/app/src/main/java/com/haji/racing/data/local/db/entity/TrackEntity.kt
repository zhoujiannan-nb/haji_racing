package com.haji.racing.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val description: String? = null,
    val type: String, // "official" or "custom"
    val startFencePointsJson: String = "[]", // JSON array of fence points
    val endFencePointsJson: String = "[]", // JSON array of fence points
    val totalDistance: Double, // meters
    val creatorUid: String? = null,
    val creatorName: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
