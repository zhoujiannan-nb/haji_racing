package com.haji.racing.core.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.haji.racing.core.gps.CoordinateConverter.wgs84ToGcj02
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

data class GpsPoint(
    val lat: Double,
    val lng: Double,
    val speed: Double, // m/s
    val accuracy: Float,
    val timestamp: Long,
)

@Singleton
class GpsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _gpsFlow = Channel<GpsPoint>(Channel.CONFLATED)
    val gpsFlow: Flow<GpsPoint> = _gpsFlow.receiveAsFlow()

    @SuppressLint("MissingPermission")
    fun startTracking() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L, // 1s interval
            0f,
            locationListener
        )
    }

    fun stopTracking() {
        locationManager.removeUpdates(locationListener)
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val (gcjLat, gcjLng) = wgs84ToGcj02(location.latitude, location.longitude)
            val point = GpsPoint(
                lat = gcjLat,
                lng = gcjLng,
                speed = location.speed.toDouble(),
                accuracy = location.accuracy,
                timestamp = System.currentTimeMillis(),
            )
            _gpsFlow.trySend(point)
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }
}
