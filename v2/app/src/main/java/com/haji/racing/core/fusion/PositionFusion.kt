package com.haji.racing.core.fusion

import com.haji.racing.core.gps.GpsPoint
import com.haji.racing.core.kalman.KalmanFilter
import com.haji.racing.core.kalman.KalmanFilter3D

data class FusedPosition(
    val lat: Double,
    val lng: Double,
    val speed: Double,
    val acceleration: FloatArray,
    val gValue: Float,
    val timestamp: Long,
)

class PositionFusion {
    private val posFilterLat = KalmanFilter(processNoise = 0.000001f, measurementNoise = 0.00001f)
    private val posFilterLng = KalmanFilter(processNoise = 0.000001f, measurementNoise = 0.00001f)
    private val speedFilter = KalmanFilter(processNoise = 0.1f, measurementNoise = 2f)
    private val accelFilter = KalmanFilter3D(processNoise = 0.05f, measurementNoise = 0.5f)

    private var lastGpsPoint: GpsPoint? = null
    private var fusedLat: Double = 0.0
    private var fusedLng: Double = 0.0

    fun init(gpsPoint: GpsPoint) {
        fusedLat = posFilterLat.update(gpsPoint.lat.toFloat()).toDouble()
        fusedLng = posFilterLng.update(gpsPoint.lng.toFloat()).toDouble()
        lastGpsPoint = gpsPoint
    }

    fun updateWithGps(gpsPoint: GpsPoint): FusedPosition {
        fusedLat = posFilterLat.update(gpsPoint.lat.toFloat()).toDouble()
        fusedLng = posFilterLng.update(gpsPoint.lng.toFloat()).toDouble()
        val filteredSpeed = speedFilter.update(gpsPoint.speed.toFloat()).toDouble()
        lastGpsPoint = gpsPoint

        return FusedPosition(
            lat = fusedLat,
            lng = fusedLng,
            speed = filteredSpeed,
            acceleration = floatArrayOf(0f, 0f, 0f),
            gValue = 0f,
            timestamp = gpsPoint.timestamp,
        )
    }

    fun updateWithAccel(
        rawAccel: FloatArray,
        gValue: Float,
        timestamp: Long,
    ): FusedPosition {
        val filtered = accelFilter.update(rawAccel[0], rawAccel[1], rawAccel[2])

        val dt = 0.04f // 25Hz = 40ms
        val currentSpeed = lastGpsPoint?.speed?.toFloat() ?: 0f
        val deltaSpeed = filtered[0] * dt // simplified forward component
        val newSpeed = currentSpeed + deltaSpeed

        return FusedPosition(
            lat = fusedLat,
            lng = fusedLng,
            speed = newSpeed.toDouble(),
            acceleration = filtered,
            gValue = gValue,
            timestamp = timestamp,
        )
    }

    fun reset() {
        posFilterLat.reset()
        posFilterLng.reset()
        speedFilter.reset()
        accelFilter.reset()
        lastGpsPoint = null
    }
}
