package com.haji.racing.ui.track

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolygonOptions

/**
 * Compose 包装的高德地图 View
 */
@Composable
fun AmapView(
    modifier: Modifier = Modifier,
    onMapReady: (AMap) -> Unit = {},
    onMapClick: (LatLng) -> Unit = {},
    onMapLongClick: (LatLng) -> Unit = {},
    onCameraMove: (LatLng) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedInstanceState = remember { Bundle() }
    val mapView = remember { createMapView(context) }

    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(savedInstanceState)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> {}
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            val map = (view as? MapView)?.getMap() ?: return@AndroidView
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false
            map.setOnMapClickListener { latLng -> onMapClick(latLng) }
            map.setOnMapLongClickListener { latLng -> onMapLongClick(latLng) }
            map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                override fun onCameraChange(position: CameraPosition?) {
                    position?.target?.let { onCameraMove(it) }
                }
                override fun onCameraChangeFinish(position: CameraPosition?) {
                    position?.target?.let { onCameraMove(it) }
                }
            })
            onMapReady(map)
        },
    )
}

private fun createMapView(context: Context): MapView {
    return MapView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }
}

/**
 * 地图操作扩展函数
 */
object AmapHelper {

    fun animateTo(aMap: AMap, latLng: LatLng, zoom: Float = 15f) {
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    fun moveCamera(aMap: AMap, latLng: LatLng, zoom: Float = 15f) {
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    fun drawPolygon(
        aMap: AMap,
        points: List<LatLng>,
        fillColor: Int,
        strokeColor: Int,
        strokeWidth: Float = 5f,
    ): com.amap.api.maps.model.Polygon? {
        if (points.size < 2) return null
        val options = PolygonOptions()
            .addAll(points)
            .fillColor(fillColor)
            .strokeColor(strokeColor)
            .strokeWidth(strokeWidth)
        return aMap.addPolygon(options)
    }

    fun clearPolygons(aMap: AMap) {
        aMap.clear()
    }

    fun addMarker(aMap: AMap, latLng: LatLng, title: String): com.amap.api.maps.model.Marker {
        val options = MarkerOptions()
            .position(latLng)
            .title(title)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        return aMap.addMarker(options)
    }

    fun clearMarkers(aMap: AMap) {
        aMap.clear()
    }
}
