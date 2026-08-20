package com.haji.racing.core.common

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.abs

/**
 * WGS84 (GPS原始坐标) 与 GCJ-02 (火星坐标系，高德/腾讯地图使用) 之间的转换工具。
 *
 * 高德地图渲染使用 GCJ-02 坐标系，而 Android 原生 LocationManager 返回的是 WGS84 坐标。
 * 在将 GPS 坐标显示到高德地图前，必须先调用 [wgs84ToGcj02] 进行转换。
 */
object CoordinateConverter {

    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    /**
     * WGS84 坐标转 GCJ-02 坐标。
     * @return Pair<纬度, 经度>，已转换为 GCJ-02
     */
    fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        if (outOfChina(lat, lng)) return lat to lng

        var dLat = transformLat(lng - 105.0, lat - 35.0)
        var dLng = transformLng(lng - 105.0, lat - 35.0)

        val radLat = lat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1.0 - EE * magic * magic
        val sqrtMagic = sqrt(magic)

        dLat = (dLat * 180.0) / ((A * (1.0 - EE)) / (magic * sqrtMagic) * PI)
        dLng = (dLng * 180.0) / (A / sqrtMagic * cos(radLat) * PI)

        return (lat + dLat) to (lng + dLng)
    }

    /**
     * GCJ-02 坐标转 WGS84 坐标（近似逆转换）。
     * @return Pair<纬度, 经度>，已转换为 WGS84
     */
    fun gcj02ToWgs84(lat: Double, lng: Double): Pair<Double, Double> {
        val (gcjLat, gcjLng) = wgs84ToGcj02(lat, lng)
        return (lat * 2 - gcjLat) to (lng * 2 - gcjLng)
    }

    private fun outOfChina(lat: Double, lng: Double): Boolean {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
