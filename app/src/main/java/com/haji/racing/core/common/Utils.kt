package com.haji.racing.core.common

import kotlin.math.*

object DistanceUtils {
    fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = toRadians(lat2 - lat1)
        val dLng = toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun toRadians(deg: Double) = deg * PI / 180.0
}

object SpeedUtils {
    fun mpsToKmh(mps: Double): Double = mps * 3.6
    fun kmhToMps(kmh: Double): Double = kmh / 3.6
}

object TimeUtils {
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun formatDurationPrecise(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val ms = durationMs % 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d.%02d".format(minutes, seconds, ms / 10)
    }
}

object FormatUtils {
    fun formatDistance(meters: Double): String = when {
        meters >= 1000 -> "%.2f km".format(meters / 1000)
        else -> "%.0f m".format(meters)
    }

    fun formatSpeed(mps: Double): String = "%.1f km/h".format(mps * 3.6)

    fun formatTimeDiff(diffMs: Long): String {
        val seconds = diffMs / 1000.0
        return if (seconds >= 0) "+%.1fs".format(seconds) else "%.1fs".format(seconds)
    }
}
