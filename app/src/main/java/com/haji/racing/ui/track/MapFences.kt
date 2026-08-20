package com.haji.racing.ui.track

import android.graphics.Color
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polygon
import com.amap.api.maps.model.PolygonOptions
import com.amap.api.maps.model.Polyline
import com.haji.racing.core.gps.GpsPoint
import com.haji.racing.domain.model.FencePoint
import com.haji.racing.domain.model.Track

/**
 * 高德地图绘制辅助：围栏、标记、实时轨迹、轨迹回放
 */
object MapFences {

    const val START_FILL = 0x552EE59D.toInt()    // 半透明绿
    const val START_STROKE = 0xFF2EE59D.toInt()  // 绿
    const val END_FILL = 0x55FF5252.toInt()      // 半透明红
    const val END_STROKE = 0xFFFF5252.toInt()    // 红
    const val ROUTE_COLOR = 0xFFFF8A3D.toInt()   // 橙色轨迹
    const val MAP_PADDING_PX = 48

    fun toAmap(points: List<FencePoint>): List<LatLng> =
        points.map { LatLng(it.lat, it.lng) }

    /** 绘制起点/终点围栏 + 中心标记 */
    fun drawTrackFences(
        aMap: AMap,
        track: Track?,
        clearFirst: Boolean = true,
    ) {
        if (clearFirst) aMap.clear()
        track ?: return
        drawFence(aMap, track.startFencePoints, START_FILL, START_STROKE, BitmapDescriptorFactory.HUE_GREEN, "起点")
        drawFence(aMap, track.endFencePoints, END_FILL, END_STROKE, BitmapDescriptorFactory.HUE_RED, "终点")
    }

    /** 创建模式：分别绘制起点/终点围栏（实时重绘） */
    fun drawZones(aMap: AMap, start: List<FencePoint>, end: List<FencePoint>) {
        aMap.clear()
        drawFence(aMap, start, START_FILL, START_STROKE, BitmapDescriptorFactory.HUE_GREEN, "起点")
        drawFence(aMap, end, END_FILL, END_STROKE, BitmapDescriptorFactory.HUE_RED, "终点")
    }

    private fun drawFence(
        aMap: AMap,
        points: List<FencePoint>,
        fill: Int,
        stroke: Int,
        hue: Float,
        title: String,
    ) {
        if (points.size < 2) return
        val latLngs = toAmap(points)
        aMap.addPolygon(
            PolygonOptions()
                .addAll(latLngs)
                .fillColor(fill)
                .strokeColor(stroke)
                .strokeWidth(4f),
        )
        val center = centroidOf(points)
        center?.let {
            aMap.addMarker(
                MarkerOptions()
                    .position(LatLng(it.lat, it.lng))
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)),
            )
        }
    }

    /** 相机移动到同时覆盖起点与终点围栏 */
    fun fitToFences(aMap: AMap, track: Track?) {
        val all = (track?.startFencePoints.orEmpty() + track?.endFencePoints.orEmpty())
        if (all.isEmpty()) return
        val lats = all.map { it.lat }
        val lngs = all.map { it.lng }
        val bounds = LatLngBounds(
            LatLng(lats.min(), lngs.min()),
            LatLng(lats.max(), lngs.max()),
        )
        aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING_PX))
    }

    /** 相机移动到指定点 */
    fun moveTo(aMap: AMap, lat: Double, lng: Double, zoom: Float = 16f) {
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom))
    }

    /** 相机覆盖一组点（用于轨迹回放） */
    fun fitToPoints(aMap: AMap, points: List<FencePoint>) {
        if (points.isEmpty()) return
        val lats = points.map { it.lat }
        val lngs = points.map { it.lng }
        val minLat = lats.min()
        val maxLat = lats.max()
        val minLng = lngs.min()
        val maxLng = lngs.max()
        // 避免单点时 bounds 退化
        val pad = 0.0005
        val bounds = LatLngBounds(
            LatLng(minLat - pad, minLng - pad),
            LatLng(maxLat + pad, maxLng + pad),
        )
        aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING_PX))
    }

    /**
     * 实时轨迹：创建/更新一条 Polyline + 当前位置 Marker
     * @return (polyline, marker)
     */
    fun attachLiveRoute(
        aMap: AMap,
    ): Pair<Polyline?, Marker?> {
        val polyline = aMap.addPolyline(
            com.amap.api.maps.model.PolylineOptions()
                .addAll(emptyList())
                .color(ROUTE_COLOR)
                .width(5f),
        )
        val marker = aMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)),
        )
        marker.position = LatLng(0.0, 0.0)
        return polyline to marker
    }

    fun updateLiveRoute(
        polyline: Polyline?,
        marker: Marker?,
        route: List<GpsPoint>,
    ) {
        if (route.isEmpty()) return
        polyline?.points = route.map { LatLng(it.lat, it.lng) }
        val last = route.last()
        marker?.position = LatLng(last.lat, last.lng)
    }

    /**
     * 轨迹回放：按速度分段染色
     * @param points (lat, lng, speedMps)
     * @return 创建的 Polyline 数量（用于调试）
     */
    fun drawTrajectory(
        aMap: AMap,
        points: List<Triple<Double, Double, Double>>,
        maxSpeed: Double,
        segmentCount: Int = 24,
    ): Int {
        if (points.size < 2) return 0
        val n = points.size
        val per = (n / segmentCount).coerceAtLeast(1)
        var count = 0
        var from = 0
        while (from < n) {
            val to = (from + per).coerceAtMost(n)
            val segment = points.subList(from, to)
            if (segment.size >= 2) {
                val avgSpeed = segment.map { it.third }.average()
                val color = com.haji.racing.core.geo.GeoMath.speedToColor(avgSpeed, maxSpeed)
                aMap.addPolyline(
                    com.amap.api.maps.model.PolylineOptions()
                        .addAll(segment.map { LatLng(it.first, it.second) })
                        .color(color)
                        .width(6f),
                )
                count++
            }
            from = to
        }
        return count
    }

    fun centroidOf(points: List<FencePoint>): FencePoint? {
        if (points.isEmpty()) return null
        return FencePoint(
            points.map { it.lat }.average(),
            points.map { it.lng }.average(),
        )
    }

    /** 空轨迹占位：避免 Color 未使用告警 */
    @Suppress("UNUSED_PARAMETER")
    private fun noop(color: Int) = Color.MAGENTA
}
