package com.haji.racing.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val totalDistance: Double = 0.0, // meters
    val totalRecordings: Int = 0,
    val isSynced: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
