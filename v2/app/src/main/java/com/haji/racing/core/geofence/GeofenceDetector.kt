package com.haji.racing.core.geofence

import kotlin.math.*

object GeofenceDetector {

    fun isInsideCircle(lat: Double, lng: Double, centerLat: Double, centerLng: Double, radiusMeters: Double): Boolean {
        val distance = haversineDistance(lat, lng, centerLat, centerLng)
        return distance <= radiusMeters
    }

    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = toRadians(lat2 - lat1)
        val dLng = toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun toRadians(deg: Double) = deg * PI / 180.0
}
