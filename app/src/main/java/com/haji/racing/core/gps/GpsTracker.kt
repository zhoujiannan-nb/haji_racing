package com.haji.racing.core.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.haji.racing.core.common.CoordinateConverter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 一个 GPS 点（GCJ-02 坐标，与高德地图显示一致）
 * @param speed m/s
 */
data class GpsPoint(
    val lat: Double,
    val lng: Double,
    val speed: Double,
    val accuracy: Float,
    val timestamp: Long,
)

/**
 * GPS 连续定位（1s 间隔，LocationManager GPS provider）。
 * 输出统一转换为 GCJ-02（wgs84ToGcj02），与围栏/地图坐标一致。
 */
@Singleton
class GpsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _gpsFlow = Channel<GpsPoint>(Channel.CONFLATED)
    val gpsFlow: Flow<GpsPoint> = _gpsFlow.receiveAsFlow()

    private val _lastPoint = MutableStateFlow<GpsPoint?>(null)
    val lastPoint: StateFlow<GpsPoint?> = _lastPoint.asStateFlow()

    private var isTracking = false

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L, // 1s interval
            0f,
            locationListener,
        )
        isTracking = true
    }

    fun stopTracking() {
        if (!isTracking) return
        locationManager.removeUpdates(locationListener)
        isTracking = false
    }

    fun isTracking(): Boolean = isTracking

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val (gcjLat, gcjLng) = CoordinateConverter.wgs84ToGcj02(
                location.latitude,
                location.longitude,
            )
            val point = GpsPoint(
                lat = gcjLat,
                lng = gcjLng,
                speed = location.speed.toDouble().coerceAtLeast(0.0),
                accuracy = location.accuracy,
                timestamp = System.currentTimeMillis(),
            )
            _lastPoint.value = point
            _gpsFlow.trySend(point)
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }
}
