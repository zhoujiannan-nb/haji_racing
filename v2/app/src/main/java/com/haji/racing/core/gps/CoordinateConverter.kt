package com.haji.racing.core.gps

import kotlin.math.PI

object CoordinateConverter {

    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        if (outOfChina(lat, lng)) return lat to lng

        var dLat = transformLat(lng - 105.0, lat - 35.0)
        var dLng = transformLng(lng - 105.0, lat - 35.0)

        val radLat = lat / 180.0 * PI
        var magic = kotlin.math.sin(radLat)
        magic = 1.0 - EE * magic * magic
        val sqrtMagic = kotlin.math.sqrt(magic)

        dLat = (dLat * 180.0) / ((A * (1.0 - EE)) / (magic * sqrtMagic) * PI)
        dLng = (dLng * 180.0) / (A / sqrtMagic * kotlin.math.cos(radLat) * PI)

        return (lat + dLat) to (lng + dLng)
    }

    private fun outOfChina(lat: Double, lng: Double): Boolean {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * kotlin.math.sqrt(kotlin.math.abs(x))
        ret += (20.0 * kotlin.math.sin(6.0 * x * PI) + 20.0 * kotlin.math.sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * kotlin.math.sin(y * PI) + 40.0 * kotlin.math.sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * kotlin.math.sin(y / 12.0 * PI) + 320.0 * kotlin.math.sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * kotlin.math.sqrt(kotlin.math.abs(x))
        ret += (20.0 * kotlin.math.sin(6.0 * x * PI) + 20.0 * kotlin.math.sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * kotlin.math.sin(x * PI) + 40.0 * kotlin.math.sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * kotlin.math.sin(x / 12.0 * PI) + 300.0 * kotlin.math.sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
