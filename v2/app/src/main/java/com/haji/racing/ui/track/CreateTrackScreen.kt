package com.haji.racing.ui.track

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
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
import com.haji.racing.core.gps.GpsPoint
import com.haji.racing.data.remote.GeocodingService
import com.haji.racing.core.gps.GpsTracker
import com.haji.racing.domain.model.FencePoint
import com.haji.racing.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CreateTrackState(
    val trackName: String = "",
    val startFencePoints: List<LatLng> = emptyList(),
    val endFencePoints: List<LatLng> = emptyList(),
    val isSaving: Boolean = false,
    val showSaveDialog: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CreateTrackViewModel @Inject constructor(
    private val trackRepository: com.haji.racing.domain.repository.TrackRepository,
    private val gpsTracker: GpsTracker,
) : ViewModel() {
    companion object {
        private const val TAG = "CreateTrackScreen"
    }

    private val _state = MutableStateFlow(CreateTrackState())
    val state: StateFlow<CreateTrackState> = _state.asStateFlow()

    private val _currentLocation = MutableStateFlow<GpsPoint?>(null)
    val currentLocation: StateFlow<GpsPoint?> = _currentLocation.asStateFlow()

    /** 搜索建议列表（Inputtips 实时联想） */
    private val _suggestions = MutableStateFlow<List<GeocodingService.SearchResult>>(emptyList())
    val suggestions: StateFlow<List<GeocodingService.SearchResult>> = _suggestions.asStateFlow()

    /** 搜索结果目标位置（选中建议或地理编码命中） */
    private val _focusLatLng = MutableStateFlow<LatLng?>(null)
    val focusLatLng: StateFlow<LatLng?> = _focusLatLng.asStateFlow()

    private var locationStarted = false
    private var searchJob: Job? = null

    fun startLocation() {
        if (!locationStarted) {
            locationStarted = true
            gpsTracker.startTracking()
            viewModelScope.launch {
                gpsTracker.gpsFlow.collect { point ->
                    _currentLocation.value = point
                }
            }
        }
    }

    fun stopLocation() {
        if (locationStarted) {
            locationStarted = false
            gpsTracker.stopTracking()
        }
    }

    // ── 搜索相关 ─────────────────────────────────────────────

    /**
     * 输入文字时实时调用 Inputtips API，带 300ms 防抖。
     * 结果通过 [suggestions] 暴露给 UI 展示下拉建议。
     */
    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        Log.d(TAG, "onSearchQueryChanged: query='$query'")
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            Log.d(TAG, "GeocodingService.searchSuggestions: query='$query'")
            val result = GeocodingService.searchSuggestions(query)
            result.onSuccess { list ->
                _suggestions.value = list
                Log.d(TAG, "Suggestions updated: ${list.size} items")
            }.onFailure { e ->
                Log.e(TAG, "searchSuggestions error", e)
                _suggestions.value = emptyList()
            }
        }
    }

    /** 选中某条建议，触发地图移动 */
    fun selectSuggestion(result: GeocodingService.SearchResult) {
        Log.d(TAG, "selectSuggestion: name=${result.name}, lat=${result.latitude}, lng=${result.longitude}")
        _focusLatLng.value = LatLng(result.latitude, result.longitude)
        _suggestions.value = emptyList()
    }

    /**
     * 地理编码搜索：用户按回车/搜索键时，对输入文字做精确地址查询。
     * 查询结果通过 [focusLatLng] 触发地图移动。
     */
    fun geocodeAddress(query: String) {
        Log.d(TAG, "geocodeAddress: query='$query'")
        _suggestions.value = emptyList()
        viewModelScope.launch {
            val result = GeocodingService.geocode(query)
            result.onSuccess { list ->
                if (list.isNotEmpty()) {
                    val first = list.first()
                    Log.d(TAG, "GeocodingService moving to: lat=${first.latitude}, lng=${first.longitude}")
                    _focusLatLng.value = LatLng(first.latitude, first.longitude)
                }
            }.onFailure { e ->
                Log.e(TAG, "geocodeAddress error", e)
            }
        }
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    // ── 赛道相关 ─────────────────────────────────────────────

    fun updateTrackName(name: String) {
        _state.value = _state.value.copy(trackName = name)
    }

    fun addStartFencePoint(point: LatLng) {
        _state.value = _state.value.copy(
            startFencePoints = _state.value.startFencePoints + point
        )
    }

    fun addEndFencePoint(point: LatLng) {
        _state.value = _state.value.copy(
            endFencePoints = _state.value.endFencePoints + point
        )
    }

    fun clearStartFence() {
        _state.value = _state.value.copy(startFencePoints = emptyList())
    }

    fun clearEndFence() {
        _state.value = _state.value.copy(endFencePoints = emptyList())
    }

    fun showSaveDialog() {
        _state.value = _state.value.copy(showSaveDialog = true)
    }

    fun hideSaveDialog() {
        _state.value = _state.value.copy(showSaveDialog = false)
    }

    fun hideSuccessDialog() {
        _state.value = _state.value.copy(showSuccessDialog = false)
    }

    fun saveTrack(creatorUid: String) {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.trackName.isBlank()) {
                _state.value = currentState.copy(errorMessage = "请输入赛道名称")
                return@launch
            }
            if (currentState.startFencePoints.size < 3) {
                _state.value = currentState.copy(errorMessage = "请至少绘制3个起点围栏点")
                return@launch
            }
            if (currentState.endFencePoints.size < 3) {
                _state.value = currentState.copy(errorMessage = "请至少绘制3个终点围栏点")
                return@launch
            }

            _state.value = currentState.copy(isSaving = true, errorMessage = null)

            try {
                // 计算起点和终点中心点之间的距离作为总距离
                val startCenter = calculateCenter(currentState.startFencePoints)
                val endCenter = calculateCenter(currentState.endFencePoints)
                val totalDistance = calculateDistance(
                    startCenter.latitude, startCenter.longitude,
                    endCenter.latitude, endCenter.longitude
                )

                val track = Track(
                    uid = UUID.randomUUID().toString(),
                    name = currentState.trackName,
                    type = "custom",
                    startFencePoints = currentState.startFencePoints.map {
                        FencePoint(it.latitude, it.longitude)
                    },
                    endFencePoints = currentState.endFencePoints.map {
                        FencePoint(it.latitude, it.longitude)
                    },
                    totalDistance = totalDistance,
                    creatorUid = creatorUid,
                    isSynced = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )

                trackRepository.saveTrack(track)
                _state.value = _state.value.copy(
                    isSaving = false,
                    showSaveDialog = false,
                    showSuccessDialog = true
                )
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    isSaving = false,
                    showSaveDialog = false,
                    errorMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    private fun calculateCenter(points: List<LatLng>): LatLng {
        val avgLat = points.sumOf { it.latitude } / points.size
        val avgLng = points.sumOf { it.longitude } / points.size
        return LatLng(avgLat, avgLng)
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

// ── UI ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTrackScreen(
    navController: NavController,
    viewModel: CreateTrackViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val focusLatLng by viewModel.focusLatLng.collectAsState()
    var drawingMode by remember { mutableStateOf(DrawingMode.NONE) }
    var aMapInstance by remember { mutableStateOf<AMap?>(null) }
    var hasMovedToCurrentLocation by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.startLocation()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // 定位成功后移动地图到当前位置（仅首次）
    LaunchedEffect(currentLocation) {
        if (!hasMovedToCurrentLocation && currentLocation != null) {
            hasMovedToCurrentLocation = true
            aMapInstance?.let { map ->
                val latLng = LatLng(currentLocation!!.lat, currentLocation!!.lng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }
        }
    }

    // 搜索结果/建议选中后移动地图
    LaunchedEffect(focusLatLng) {
        focusLatLng?.let { latLng ->
            aMapInstance?.let { map ->
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLocation()
            viewModel.clearSuggestions()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建赛道") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showSaveDialog() },
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
            // 搜索栏（通过高德 Web API 实时建议下拉）
            TrackSearchBar(
                query = searchQuery,
                onQueryChange = { query ->
                    searchQuery = query
                    viewModel.onSearchQueryChanged(query)
                },
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        viewModel.geocodeAddress(query)
                    }
                },
                suggestions = suggestions,
                onSuggestionClick = { result ->
                    viewModel.selectSuggestion(result)
                    searchQuery = result.name
                },
                onDismissSuggestions = { viewModel.clearSuggestions() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 地图 + 悬浮定位按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
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
                    modifier = Modifier.fillMaxSize()
                )

                // 悬浮定位按钮
                FloatingActionButton(
                    onClick = {
                        currentLocation?.let { loc ->
                            aMapInstance?.let { map ->
                                val latLng = LatLng(loc.lat, loc.lng)
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "定位到当前位置",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

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
    
    // 保存赛道对话框
    if (state.showSaveDialog) {
        SaveTrackDialog(
            trackName = state.trackName,
            onNameChange = { viewModel.updateTrackName(it) },
            onConfirm = { viewModel.saveTrack("current-user-uid") },
            onDismiss = { viewModel.hideSaveDialog() },
            isSaving = state.isSaving
        )
    }
    
    // 保存成功对话框
    if (state.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideSuccessDialog() },
            title = { Text("保存成功") },
            text = { Text("赛道已成功保存！") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.hideSuccessDialog()
                    navController.navigateUp()
                }) {
                    Text("确定")
                }
            }
        )
    }
}

// ── 辅助类型 ───────────────────────────────────────────────

enum class DrawingMode {
    NONE, START, END
}

// ── 地图相关函数 ───────────────────────────────────────────

private fun createMapView(context: Context): MapView {
    val mapView = MapView(context)
    mapView.onCreate(null)
    mapView.onResume()

    val aMap = mapView.map
    aMap.uiSettings.isZoomControlsEnabled = false
    aMap.uiSettings.isCompassEnabled = true
    aMap.uiSettings.isMyLocationButtonEnabled = false
    aMap.mapType = AMap.MAP_TYPE_NORMAL

    val myLocationStyle = MyLocationStyle().apply {
        myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE)
        interval(2000)
        showMyLocation(true)
    }
    aMap.myLocationStyle = myLocationStyle
    aMap.isMyLocationEnabled = true

    val defaultLatLng = LatLng(39.9042, 116.4074)
    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 12f))

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
                viewModel.addStartFencePoint(latLng)
                redrawAllFences(aMap, viewModel.state.value)
            }
            DrawingMode.END -> {
                viewModel.addEndFencePoint(latLng)
                redrawAllFences(aMap, viewModel.state.value)
            }
            DrawingMode.NONE -> { /* 无操作 */ }
        }
    }
}

private fun redrawAllFences(aMap: AMap, state: CreateTrackState) {
    aMap.clear()
    
    // 绘制起点围栏
    drawStartFencePoints(aMap, state.startFencePoints)
    
    // 绘制终点围栏
    drawEndFencePoints(aMap, state.endFencePoints)
}

private fun drawStartFencePoints(aMap: AMap, points: List<LatLng>) {
    if (points.isEmpty()) return
    
    // 绘制点
    points.forEachIndexed { index, point ->
        aMap.addMarker(
            MarkerOptions()
                .position(point)
                .title("起点围栏点 ${index + 1}")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
    }
    
    // 如果有3个或以上点，绘制多边形
    if (points.size >= 3) {
        aMap.addPolygon(
            PolygonOptions()
                .addAll(points)
                .strokeColor(Color.GREEN)
                .fillColor(Color.argb(50, 0, 255, 0))
                .strokeWidth(3f)
        )
        
        // 绘制方向箭头（在第一条边上）
        drawDirectionArrow(aMap, points[0], points[1], Color.GREEN)
    } else if (points.size == 2) {
        // 只有2个点时绘制线段
        aMap.addPolyline(
            PolylineOptions()
                .add(points[0], points[1])
                .color(Color.GREEN)
                .width(4f)
        )
    }
}

private fun drawEndFencePoints(aMap: AMap, points: List<LatLng>) {
    if (points.isEmpty()) return
    
    // 绘制点
    points.forEachIndexed { index, point ->
        aMap.addMarker(
            MarkerOptions()
                .position(point)
                .title("终点围栏点 ${index + 1}")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }
    
    // 如果有3个或以上点，绘制多边形
    if (points.size >= 3) {
        aMap.addPolygon(
            PolygonOptions()
                .addAll(points)
                .strokeColor(Color.RED)
                .fillColor(Color.argb(50, 255, 0, 0))
                .strokeWidth(3f)
        )
        
        // 绘制方向箭头（在最后一条边上，指向内部）
        drawDirectionArrow(aMap, points[points.size - 1], points[0], Color.RED)
    } else if (points.size == 2) {
        // 只有2个点时绘制线段
        aMap.addPolyline(
            PolylineOptions()
                .add(points[0], points[1])
                .color(Color.RED)
                .width(4f)
        )
    }
}

private fun drawDirectionArrow(aMap: AMap, from: LatLng, to: LatLng, color: Int) {
    // 计算方向角度
    val angle = Math.atan2(
        to.longitude - from.longitude,
        to.latitude - from.latitude
    )
    
    // 计算箭头位置（线段中点）
    val midLat = (from.latitude + to.latitude) / 2
    val midLng = (from.longitude + to.longitude) / 2
    val midPoint = LatLng(midLat, midLng)
    
    // 添加箭头标记
    aMap.addMarker(
        MarkerOptions()
            .position(midPoint)
            .title("方向")
            .snippet("进入方向")
            .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_media_play))
            .anchor(0.5f, 0.5f)
            .rotateAngle(Math.toDegrees(angle).toFloat())
    )
}

// ── 搜索栏组件 ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    suggestions: List<GeocodingService.SearchResult>,
    onSuggestionClick: (GeocodingService.SearchResult) -> Unit,
    onDismissSuggestions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = { onSearch(query) }) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
            },
            label = { Text("搜索地址或地点") },
            placeholder = { Text("输入地名搜索") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            singleLine = true,
        )

        // Inputtips 实时建议下拉菜单
        DropdownMenu(
            expanded = suggestions.isNotEmpty(),
            onDismissRequest = onDismissSuggestions,
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { result ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = result.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            result.address?.let { address ->
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = { onSuggestionClick(result) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

// ── 保存赛道对话框 ─────────────────────────────────────────

@Composable
private fun SaveTrackDialog(
    trackName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean,
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("保存赛道") },
        text = {
            OutlinedTextField(
                value = trackName,
                onValueChange = onNameChange,
                label = { Text("赛道名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSaving
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = trackName.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("取消")
            }
        }
    )
}
