package com.haji.racing.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val type: String, // "official" or "custom"
    val startFencePointsJson: String = "[]", // JSON array of fence points
    val endFencePointsJson: String = "[]", // JSON array of fence points
    val startDirectionFromJson: String? = null, // JSON of start direction from point
    val startDirectionToJson: String? = null, // JSON of start direction to point
    val endDirectionFromJson: String? = null, // JSON of end direction from point
    val endDirectionToJson: String? = null, // JSON of end direction to point
    val totalDistance: Double, // meters
    val creatorUid: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
