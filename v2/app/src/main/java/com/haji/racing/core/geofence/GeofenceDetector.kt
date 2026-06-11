package com.haji.racing.core.geofence

import com.haji.racing.domain.model.FencePoint
import kotlin.math.*

object GeofenceDetector {

    fun isInsideCircle(lat: Double, lng: Double, centerLat: Double, centerLng: Double, radiusMeters: Double): Boolean {
        val distance = haversineDistance(lat, lng, centerLat, centerLng)
        return distance <= radiusMeters
    }

    fun isInsidePolygon(lat: Double, lng: Double, polygon: List<FencePoint>): Boolean {
        if (polygon.size < 3) return false
        
        var inside = false
        var j = polygon.size - 1
        
        for (i in polygon.indices) {
            val xi = polygon[i].lng
            val yi = polygon[i].lat
            val xj = polygon[j].lng
            val yj = polygon[j].lat
            
            val intersect = ((yi > lat) != (yj > lat)) &&
                (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi)
            
            if (intersect) inside = !inside
            j = i
        }
        
        return inside
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
