package com.haji.racing.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.haji.racing.data.local.db.HajiRacingDatabase
import com.haji.racing.data.local.db.dao.*
import com.haji.racing.data.remote.api.AuthInterceptor
import com.haji.racing.data.remote.api.HajiApi
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
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
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
    @Provides fun provideUserDao(db: HajiRacingDatabase): UserDao = db.userDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://haji-racing.online:8443/"

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): HajiApi =
        retrofit.create(HajiApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

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
