package com.haji.racing.ui.track

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeResult
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.model.TrackPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CreateTrackState(
    val trackName: String = "",
    val startPoint: LatLonPoint? = null,
    val endPoint: LatLonPoint? = null,
    val startFenceRadius: Double = 50.0,
    val endFenceRadius: Double = 50.0,
    val trackPoints: List<TrackPoint> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CreateTrackViewModel @Inject constructor(
    private val trackRepository: com.haji.racing.domain.repository.TrackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateTrackState())
    val state: StateFlow<CreateTrackState> = _state.asStateFlow()

    fun updateTrackName(name: String) {
        _state.value = _state.value.copy(trackName = name)
    }

    fun setStartPoint(point: LatLonPoint, radius: Double = 50.0) {
        _state.value = _state.value.copy(
            startPoint = point,
            startFenceRadius = radius
        )
    }

    fun setEndPoint(point: LatLonPoint, radius: Double = 50.0) {
        _state.value = _state.value.copy(
            endPoint = point,
            endFenceRadius = radius
        )
    }

    fun addTrackPoint(point: TrackPoint) {
        _state.value = _state.value.copy(
            trackPoints = _state.value.trackPoints + point
        )
    }

    fun clearTrackPoints() {
        _state.value = _state.value.copy(trackPoints = emptyList())
    }

    fun saveTrack(creatorUid: String) {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.trackName.isBlank()) {
                _state.value = currentState.copy(errorMessage = "请输入赛道名称")
                return@launch
            }
            if (currentState.startPoint == null) {
                _state.value = currentState.copy(errorMessage = "请设置起点")
                return@launch
            }
            if (currentState.endPoint == null) {
                _state.value = currentState.copy(errorMessage = "请设置终点")
                return@launch
            }
            if (currentState.trackPoints.size < 2) {
                _state.value = currentState.copy(errorMessage = "请至少绘制2个赛道点位")
                return@launch
            }

            _state.value = currentState.copy(isSaving = true, errorMessage = null)

            try {
                // 计算总距离
                var totalDistance = 0.0
                for (i in 1 until currentState.trackPoints.size) {
                    val prev = currentState.trackPoints[i - 1]
                    val curr = currentState.trackPoints[i]
                    totalDistance += calculateDistance(
                        prev.lat, prev.lng,
                        curr.lat, curr.lng
                    )
                }

                val track = Track(
                    uid = UUID.randomUUID().toString(),
                    name = currentState.trackName,
                    type = "custom",
                    startLat = currentState.startPoint!!.latitude,
                    startLng = currentState.startPoint!!.longitude,
                    startFenceRadius = currentState.startFenceRadius,
                    endLat = currentState.endPoint!!.latitude,
                    endLng = currentState.endPoint!!.longitude,
                    endFenceRadius = currentState.endFenceRadius,
                    totalDistance = totalDistance,
                    creatorUid = creatorUid,
                    isSynced = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    points = currentState.trackPoints,
                )

                trackRepository.saveTrack(track)
                
                _state.value = _state.value.copy(isSaving = false)
                // TODO: 导航返回成功
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    isSaving = false,
                    errorMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTrackScreen(
    navController: NavController,
    viewModel: CreateTrackViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var drawingMode by remember { mutableStateOf(DrawingMode.NONE) }
    var aMapInstance by remember { mutableStateOf<AMap?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 权限结果处理
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建赛道") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // TODO: 获取 creatorUid
                            viewModel.saveTrack("current-user-uid")
                        },
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "保存")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 赛道名称输入
            OutlinedTextField(
                value = state.trackName,
                onValueChange = { viewModel.updateTrackName(it) },
                label = { Text("赛道名称") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { /* 关闭键盘 */ }),
                singleLine = true,
            )

            // 搜索栏
            SearchBar(
                onSearch = { query ->
                    aMapInstance?.let { map ->
                        searchAddress(context, query, map)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 地图视图
            AndroidView(
                factory = { ctx ->
                    val mapView = createMapView(ctx)
                    aMapInstance = mapView.map
                    mapView
                },
                update = { mapView ->
                    aMapInstance = mapView.map
                    setupMapInteraction(mapView.map, drawingMode, viewModel)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // 绘制模式选择
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { drawingMode = DrawingMode.START },
                    modifier = Modifier.weight(1f),
                    enabled = drawingMode != DrawingMode.START
                ) {
                    Text("设置起点")
                }
                Button(
                    onClick = { drawingMode = DrawingMode.END },
                    modifier = Modifier.weight(1f),
                    enabled = drawingMode != DrawingMode.END
                ) {
                    Text("设置终点")
                }
                Button(
                    onClick = { drawingMode = DrawingMode.TRACK },
                    modifier = Modifier.weight(1f),
                    enabled = drawingMode != DrawingMode.TRACK
                ) {
                    Text("绘制赛道")
                }
            }

            // 错误提示
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

enum class DrawingMode {
    NONE, START, END, TRACK
}

private fun createMapView(context: Context): MapView {
    val mapView = MapView(context)
    mapView.onCreate(null)
    mapView.onResume()

    val aMap = mapView.map
    aMap.uiSettings.isZoomControlsEnabled = true
    aMap.uiSettings.isCompassEnabled = true
    aMap.mapType = AMap.MAP_TYPE_NORMAL

    // 移动到默认位置（北京）
    val defaultPosition = LatLng(39.9042, 116.4074)
    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, 15f))

    return mapView
}

private fun setupMapInteraction(
    aMap: AMap,
    drawingMode: DrawingMode,
    viewModel: CreateTrackViewModel,
) {
    aMap.setOnMapClickListener { latLng ->
        when (drawingMode) {
            DrawingMode.START -> {
                // 添加起点标记
                aMap.clear()
                aMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("起点")
                        .snippet("半径: 50米")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
                
                // 添加起点围栏圆圈
                aMap.addCircle(
                    CircleOptions()
                        .center(latLng)
                        .radius(50.0)
                        .strokeColor(android.graphics.Color.BLUE)
                        .fillColor(android.graphics.Color.argb(50, 0, 0, 255))
                        .strokeWidth(2f)
                )

                viewModel.setStartPoint(LatLonPoint(latLng.latitude, latLng.longitude))
            }
            DrawingMode.END -> {
                // 添加终点标记
                aMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("终点")
                        .snippet("半径: 50米")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )

                // 添加终点围栏圆圈
                aMap.addCircle(
                    CircleOptions()
                        .center(latLng)
                        .radius(50.0)
                        .strokeColor(android.graphics.Color.RED)
                        .fillColor(android.graphics.Color.argb(50, 255, 0, 0))
                        .strokeWidth(2f)
                )

                viewModel.setEndPoint(LatLonPoint(latLng.latitude, latLng.longitude))
            }
            DrawingMode.TRACK -> {
                // 添加赛道点
                viewModel.addTrackPoint(
                    TrackPoint(
                        lat = latLng.latitude,
                        lng = latLng.longitude,
                        sequenceIndex = viewModel.state.value.trackPoints.size
                    )
                )

                // 在地图上显示点
                aMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
            }
            DrawingMode.NONE -> {
                // 无操作
            }
        }
    }
}

private fun searchAddress(context: Context, query: String, aMap: AMap) {
    val geocodeSearch = GeocodeSearch(context)
    geocodeSearch.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
        override fun onRegeocodeSearched(regeocodeResult: RegeocodeResult, resultCode: Int) {
            // 逆地理编码结果
        }

        override fun onGeocodeSearched(geocodeResult: GeocodeResult, resultCode: Int) {
            if (resultCode == 1000 && geocodeResult != null && geocodeResult.geocodeAddressList.isNotEmpty()) {
                val geocodeAddress = geocodeResult.geocodeAddressList[0]
                val latLonPoint = geocodeAddress.latLonPoint
                val latLng = LatLng(latLonPoint.latitude, latLonPoint.longitude)
                
                // 移动地图到搜索结果
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                
                // 添加标记
                aMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("搜索结果")
                        .snippet(geocodeAddress.formatAddress)
                )
            }
        }
    })
    
    val query = GeocodeQuery(query, "全国")
    geocodeSearch.getFromLocationNameAsyn(query)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onSearch(query) }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "搜索")
                }
            }
        },
        label = { Text("搜索地址") },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        singleLine = true,
    )
}
