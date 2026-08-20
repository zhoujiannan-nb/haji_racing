package com.haji.racing.ui.track

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptor
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

    /** 绘制起点/终点围栏 + 编号图钉 */
    fun drawTrackFences(
        context: Context,
        aMap: AMap,
        track: Track?,
        clearFirst: Boolean = true,
    ) {
        if (clearFirst) aMap.clear()
        track ?: return
        drawFence(context, aMap, track.startFencePoints, START_FILL, START_STROKE, "起")
        drawFence(context, aMap, track.endFencePoints, END_FILL, END_STROKE, "终")
    }

    /** 创建模式：分别绘制起点/终点围栏（实时重绘） */
    fun drawZones(context: Context, aMap: AMap, start: List<FencePoint>, end: List<FencePoint>) {
        aMap.clear()
        drawFence(context, aMap, start, START_FILL, START_STROKE, "起")
        drawFence(context, aMap, end, END_FILL, END_STROKE, "终")
    }

    /**
     * 绘制一个围栏区域：
     * - 每个顶点一个编号图钉（起1、起2… / 终1、终2…），1 个点起就可见
     * - 2 个点画连线，≥3 个点闭合多边形
     */
    private fun drawFence(
        context: Context,
        aMap: AMap,
        points: List<FencePoint>,
        fill: Int,
        stroke: Int,
        prefix: String,
    ) {
        val latLngs = toAmap(points)

        // 顶点编号图钉（始终绘制）
        points.forEachIndexed { i, p ->
            aMap.addMarker(
                MarkerOptions()
                    .position(LatLng(p.lat, p.lng))
                    .title("$prefix${i + 1}")
                    .icon(textPin(context, "$prefix${i + 1}", stroke))
                    .anchor(0.5f, 1f)
                    .zIndex(10f),
            )
        }

        if (points.size == 2) {
            aMap.addPolyline(
                com.amap.api.maps.model.PolylineOptions()
                    .addAll(latLngs)
                    .color(stroke)
                    .width(4f),
            )
        } else if (points.size >= 3) {
            aMap.addPolygon(
                PolygonOptions()
                    .addAll(latLngs)
                    .fillColor(fill)
                    .strokeColor(stroke)
                    .strokeWidth(4f),
            )
        }
    }

    /** 文字编号图钉：彩色圆钉 + 白色编号，锚点在针尖 */
    private fun textPin(context: Context, label: String, color: Int): BitmapDescriptor {
        val density = context.resources.displayMetrics.density
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            textSize = 14f * density
        }
        val padX = 9f * density
        val textW = textPaint.measureText(label)
        val w = (textW + padX * 2).toInt().coerceAtLeast((40 * density).toInt())
        val r = w / 2f - 2f * density          // 圆形半径
        val h = (r * 2 + 14f * density).toInt() // 圆 + 针尖
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val body = Path()
        body.addCircle(w / 2f, w / 2f, r, Path.Direction.CW)
        val tip = Path()
        tip.moveTo(w / 2f - 11f * density, w / 2f + r * 0.72f)
        tip.lineTo(w / 2f + 11f * density, w / 2f + r * 0.72f)
        tip.lineTo(w / 2f, h - 1f * density)
        tip.close()

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.BLACK
            alpha = 90
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        c.drawPath(body, fillPaint)
        c.drawPath(tip, fillPaint)
        c.drawPath(body, strokePaint)
        c.drawPath(tip, strokePaint)

        val y = w / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        c.drawText(label, w / 2f, y, textPaint)
        return BitmapDescriptorFactory.fromBitmap(bmp)
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
