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
import com.haji.racing.domain.model.Track
import com.haji.racing.domain.model.TrackPoint
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
    val startPoint: GeocodingService.GeoPoint? = null,
    val endPoint: GeocodingService.GeoPoint? = null,
    val startFenceRadius: Double = 50.0,
    val endFenceRadius: Double = 50.0,
    val trackPoints: List<TrackPoint> = emptyList(),
    val isSaving: Boolean = false,
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

    fun setStartPoint(point: GeocodingService.GeoPoint, radius: Double = 50.0) {
        _state.value = _state.value.copy(startPoint = point, startFenceRadius = radius)
    }

    fun setEndPoint(point: GeocodingService.GeoPoint, radius: Double = 50.0) {
        _state.value = _state.value.copy(endPoint = point, endFenceRadius = radius)
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
                var totalDistance = 0.0
                for (i in 1 until currentState.trackPoints.size) {
                    val prev = currentState.trackPoints[i - 1]
                    val curr = currentState.trackPoints[i]
                    totalDistance += calculateDistance(prev.lat, prev.lng, curr.lat, curr.lng)
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
                        onClick = { viewModel.saveTrack("current-user-uid") },
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

// ── 辅助类型 ───────────────────────────────────────────────

enum class DrawingMode {
    NONE, START, END, TRACK
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
                aMap.clear()
                aMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("起点")
                        .snippet("半径: 50米")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
                aMap.addCircle(
                    CircleOptions()
                        .center(latLng)
                        .radius(50.0)
                        .strokeColor(Color.BLUE)
                        .fillColor(Color.argb(50, 0, 0, 255))
                        .strokeWidth(2f)
                )
                viewModel.setStartPoint(GeocodingService.GeoPoint(latLng.latitude, latLng.longitude))
            }
            DrawingMode.END -> {
                aMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("终点")
                        .snippet("半径: 50米")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                aMap.addCircle(
                    CircleOptions()
                        .center(latLng)
                        .radius(50.0)
                        .strokeColor(Color.RED)
                        .fillColor(Color.argb(50, 255, 0, 0))
                        .strokeWidth(2f)
                )
                viewModel.setEndPoint(GeocodingService.GeoPoint(latLng.latitude, latLng.longitude))
            }
            DrawingMode.TRACK -> {
                viewModel.addTrackPoint(
                    TrackPoint(
                        lat = latLng.latitude,
                        lng = latLng.longitude,
                        sequenceIndex = viewModel.state.value.trackPoints.size
                    )
                )
                aMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
            }
            DrawingMode.NONE -> { /* 无操作 */ }
        }
    }
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
