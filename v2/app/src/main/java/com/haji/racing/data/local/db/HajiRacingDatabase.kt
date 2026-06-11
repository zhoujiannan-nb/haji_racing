package com.haji.racing.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.haji.racing.data.local.db.dao.*
import com.haji.racing.data.local.db.entity.*

@Database(
    entities = [
        TrackEntity::class,
        TrackPointEntity::class,
        RecordingEntity::class,
        RecordingPointEntity::class,
        UserEntity::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class HajiRacingDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun recordingDao(): RecordingDao
    abstract fun recordingPointDao(): RecordingPointDao
    abstract fun userDao(): UserDao
}
