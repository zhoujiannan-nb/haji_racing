package com.haji.racing.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.haji.racing.data.local.db.dao.RecordingDao
import com.haji.racing.data.local.db.dao.RecordingPointDao
import com.haji.racing.data.local.db.dao.TrackDao
import com.haji.racing.data.local.db.entity.RecordingEntity
import com.haji.racing.data.local.db.entity.RecordingPointEntity
import com.haji.racing.data.local.db.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        RecordingEntity::class,
        RecordingPointEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class HajiRacingDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun recordingDao(): RecordingDao
    abstract fun recordingPointDao(): RecordingPointDao
}
