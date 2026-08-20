package com.haji.racing.core.geo

import com.haji.racing.domain.model.FencePoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 地理计算工具（坐标统一 GCJ-02，距离/角度计算与坐标系无关）
 */
object GeoMath {

    const val EARTH_RADIUS = 6371000.0

    /** Haversine 距离（米） */
    fun distance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = toRadians(lat2 - lat1)
        val dLng = toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS * c
    }

    /** 点是否在多边形内（射线法） */
    fun isPointInPolygon(lat: Double, lng: Double, polygon: List<FencePoint>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].lng
            val yi = polygon[i].lat
            val xj = polygon[j].lng
            val yj = polygon[j].lat
            val intersect = (yi > lat) != (yj > lat) &&
                (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    /** 点到多边形的最短距离（米），多边形为空时返回 null */
    fun distanceToPolygon(lat: Double, lng: Double, polygon: List<FencePoint>): Double? {
        if (polygon.isEmpty()) return null
        var minDistance = Double.MAX_VALUE
        for (i in polygon.indices) {
            val p1 = polygon[i]
            val p2 = polygon[(i + 1) % polygon.size]
            val d = distanceToSegment(lat, lng, p1.lat, p1.lng, p2.lat, p2.lng)
            if (d < minDistance) minDistance = d
        }
        return minDistance
    }

    /** 多边形质心（简单平均） */
    fun centroid(polygon: List<FencePoint>): FencePoint? {
        if (polygon.isEmpty()) return null
        val lat = polygon.map { it.lat }.average()
        val lng = polygon.map { it.lng }.average()
        return FencePoint(lat, lng)
    }

    /** 起点围栏中心到终点围栏中心的估算赛道距离（米） */
    fun estimateTrackLength(start: List<FencePoint>, end: List<FencePoint>): Double {
        val c1 = centroid(start) ?: return 0.0
        val c2 = centroid(end) ?: return 0.0
        return distance(c1.lat, c1.lng, c2.lat, c2.lng)
    }

    /** 轨迹累计里程（米） */
    fun pathLength(points: List<FencePoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += distance(points[i].lat, points[i].lng, points[i + 1].lat, points[i + 1].lng)
        }
        return total
    }

    /** 速度 -> 颜色（慢=青蓝，中=黄，快=橙红），用于轨迹染色 */
    fun speedToColor(speedMps: Double, maxSpeedMps: Double): Int {
        if (maxSpeedMps <= 0.1) return android.graphics.Color.rgb(76, 201, 240)
        val t = (speedMps / maxSpeedMps).coerceIn(0.0, 1.0)
        // 200(蓝) -> 45(黄) -> 12(红)
        val hue = if (t < 0.5f) {
            (200.0 - t * 2 * (200.0 - 45.0)).toFloat()
        } else {
            (45.0 - (t - 0.5) * 2 * (45.0 - 12.0)).toFloat()
        }
        return android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.9f, 0.95f))
    }

    private fun distanceToSegment(
        pointLat: Double,
        pointLng: Double,
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
    ): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val l2 = dx * dx + dy * dy
        if (l2 == 0.0) return distance(pointLat, pointLng, x1, y1)
        val t = ((pointLat - x1) * dx + (pointLng - y1) * dy) / l2
        val tClamped = t.coerceIn(0.0, 1.0)
        val closestLat = x1 + tClamped * dx
        val closestLng = y1 + tClamped * dy
        return distance(pointLat, pointLng, closestLat, closestLng)
    }

    private fun toRadians(deg: Double): Double = deg * PI / 180.0
}
