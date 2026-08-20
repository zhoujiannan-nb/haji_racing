package com.haji.racing.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val description: String? = null,
    val startFencePointsJson: String = "[]", // JSON array of fence points (GCJ-02)
    val endFencePointsJson: String = "[]",   // JSON array of fence points (GCJ-02)
    val totalDistance: Double, // meters, start->end estimate
    val creatorName: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
