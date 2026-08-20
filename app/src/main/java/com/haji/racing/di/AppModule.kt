package com.haji.racing.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.haji.racing.data.local.db.HajiRacingDatabase
import com.haji.racing.data.local.db.dao.RecordingDao
import com.haji.racing.data.local.db.dao.RecordingPointDao
import com.haji.racing.data.local.db.dao.TrackDao
import com.haji.racing.data.remote.api.AmapApi
import com.haji.racing.data.repository.RecordingRepositoryImpl
import com.haji.racing.data.repository.TrackRepositoryImpl
import com.haji.racing.domain.repository.RecordingRepository
import com.haji.racing.domain.repository.TrackRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    @Provides fun provideRecordingDao(db: HajiRacingDatabase): RecordingDao = db.recordingDao()
    @Provides fun provideRecordingPointDao(db: HajiRacingDatabase): RecordingPointDao = db.recordingPointDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 高德 Web 服务 API（POI 搜索，无认证拦截器）
    private const val AMAP_BASE_URL = "https://restapi.amap.com/"

    @Provides
    @Singleton
    fun provideAmapRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(AMAP_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAmapApi(amapRetrofit: Retrofit): AmapApi =
        amapRetrofit.create(AmapApi::class.java)

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
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
}
