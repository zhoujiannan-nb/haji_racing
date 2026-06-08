package com.haji.racing.di

import android.content.Context
import androidx.room.Room
import com.haji.racing.data.local.db.HajiRacingDatabase
import com.haji.racing.data.local.db.dao.*
import com.haji.racing.data.remote.api.HajiApi
import com.haji.racing.data.remote.api.MockHajiApi
import com.haji.racing.data.repository.RecordingRepositoryImpl
import com.haji.racing.data.repository.TrackRepositoryImpl
import com.haji.racing.data.repository.UserRepositoryImpl
import com.haji.racing.domain.repository.RecordingRepository
import com.haji.racing.domain.repository.TrackRepository
import com.haji.racing.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HajiRacingDatabase =
        Room.databaseBuilder(context, HajiRacingDatabase::class.java, "haji_racing.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTrackDao(db: HajiRacingDatabase): TrackDao = db.trackDao()
    @Provides fun provideTrackPointDao(db: HajiRacingDatabase): TrackPointDao = db.trackPointDao()
    @Provides fun provideRecordingDao(db: HajiRacingDatabase): RecordingDao = db.recordingDao()
    @Provides fun provideRecordingPointDao(db: HajiRacingDatabase): RecordingPointDao = db.recordingPointDao()
    @Provides fun provideUserDao(db: HajiRacingDatabase): UserDao = db.userDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApi(): HajiApi = MockHajiApi()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTrackRepository(impl: TrackRepositoryImpl): TrackRepository = impl

    @Provides
    @Singleton
    fun provideRecordingRepository(impl: RecordingRepositoryImpl): RecordingRepository = impl

    @Provides
    @Singleton
    fun provideUserRepository(impl: UserRepositoryImpl): UserRepository = impl
}
