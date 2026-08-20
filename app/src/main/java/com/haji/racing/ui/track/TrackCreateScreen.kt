package com.haji.racing.ui.track

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.amap.api.maps.AMap
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.core.geo.GeoMath
import com.haji.racing.domain.model.FencePoint
import com.haji.racing.ui.components.RacingButton
import com.haji.racing.ui.components.RacingTextField
import com.haji.racing.ui.theme.RacingAccent
import com.haji.racing.ui.theme.RacingCard
import com.haji.racing.ui.theme.RacingCardElevated
import com.haji.racing.ui.theme.RacingEnd
import com.haji.racing.ui.theme.RacingOutline
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.RacingStart
import com.haji.racing.ui.theme.TextPrimary
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.theme.TextTertiary

@Composable
fun TrackCreateScreen(
    navController: NavController,
    viewModel: TrackCreateViewModel = hiltViewModel(),
) {
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val poiResults by viewModel.poiResults.collectAsStateWithLifecycle()
    val poiSearching by viewModel.poiSearching.collectAsStateWithLifecycle()

    var mapRef by remember { mutableStateOf<AMap?>(null) }
    var mapReady by remember { mutableStateOf(false) }

    var startPoints by remember { mutableStateOf<List<FencePoint>>(emptyList()) }
    var endPoints by remember { mutableStateOf<List<FencePoint>>(emptyList()) }
    var editingZone by remember { mutableStateOf(0) } // 0 起点 / 1 终点

    var showSaveSheet by remember { mutableStateOf(false) }
    var showPoiSheet by remember { mutableStateOf(false) }
    var poiQuery by remember { mutableStateOf("") }
    var trackName by remember { mutableStateOf("") }
    var trackDesc by remember { mutableStateOf("") }

    val canSave = startPoints.size >= 3 && endPoints.size >= 3

    fun addFencePoint(latLng: com.amap.api.maps.model.LatLng) {
        val point = FencePoint(latLng.latitude, latLng.longitude)
        if (editingZone == 0) startPoints = startPoints + point else endPoints = endPoints + point
    }

    // 地图就绪后重绘围栏
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(startPoints, endPoints, mapReady) {
        if (mapReady) {
            val aMap = mapRef ?: return@LaunchedEffect
            MapFences.drawZones(context, aMap, startPoints, endPoints)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ===== 地图 =====
        AmapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { aMap ->
                if (mapReady) return@AmapView
                mapReady = true
                mapRef = aMap
                // 初始视角：杭州（示例区域），用户可拖动
                MapFences.moveTo(aMap, 30.2421, 120.1536, 13f)
            },
            onMapClick = { latLng -> addFencePoint(latLng) },
            onMapLongClick = { latLng -> addFencePoint(latLng) },
        )

        // ===== 顶部栏 =====
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(0.dp, 0.dp, 24.dp, 24.dp))
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(RacingCardElevated.copy(alpha = 0.98f), RacingCardElevated.copy(alpha = 0.86f)),
                ))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "创建赛道",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "点击地图添加围栏点",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
                TextButton(
                    onClick = { showSaveSheet = true },
                    enabled = canSave,
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = null,
                        tint = if (canSave) RacingPrimary else TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "保存",
                        color = if (canSave) RacingPrimary else TextTertiary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // ===== 底部工具栏 =====
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(RacingCardElevated.copy(alpha = 0.98f), RacingCard.copy(alpha = 0.96f)),
                    ),
                )
                .border(1.dp, RacingOutline, RoundedCornerShape(24.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 区域切换
            ZoneToggle(
                selected = editingZone,
                startCount = startPoints.size,
                endCount = endPoints.size,
                onSelect = { editingZone = it },
            )
            Spacer(Modifier.width(10.dp))
            IconButton(onClick = {
                if (editingZone == 0) {
                    if (startPoints.isNotEmpty()) startPoints = startPoints.dropLast(1)
                } else {
                    if (endPoints.isNotEmpty()) endPoints = endPoints.dropLast(1)
                }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "撤销",
                    tint = TextSecondary,
                )
            }
            IconButton(onClick = {
                if (editingZone == 0) startPoints = emptyList() else endPoints = emptyList()
            }) {
                Icon(
                    Icons.Filled.Deselect,
                    contentDescription = "清空",
                    tint = TextSecondary,
                )
            }
            Spacer(Modifier.weight(1f))
            // POI 搜索
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(RacingSurfaceDark)
                    .clickable { showPoiSheet = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = RacingAccent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "搜位置",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
    }

    // ===== 保存 BottomSheet =====
    if (showSaveSheet) {
        SaveTrackSheet(
            name = trackName,
            onNameChange = { trackName = it },
            description = trackDesc,
            onDescriptionChange = { trackDesc = it },
            distance = GeoMath.estimateTrackLength(startPoints, endPoints),
            creatorName = nickname,
            canSave = canSave && trackName.isNotBlank(),
            onSave = {
                viewModel.saveTrack(trackName, trackDesc, startPoints, endPoints)
                showSaveSheet = false
                navController.popBackStack()
            },
            onDismiss = { showSaveSheet = false },
        )
    }

    // ===== POI 搜索 BottomSheet =====
    if (showPoiSheet) {
        PoiSearchSheet(
            query = poiQuery,
            onQueryChange = { poiQuery = it },
            searching = poiSearching,
            results = poiResults,
            onSearch = {
                val center = mapRef?.cameraPosition?.target
                viewModel.searchPoi(poiQuery, center?.let { it.latitude to it.longitude })
            },
            onPick = { poi ->
                val aMap = mapRef
                if (aMap != null) {
                    MapFences.moveTo(aMap, poi.lat, poi.lng, 16f)
                }
                val point = FencePoint(poi.lat, poi.lng)
                if (editingZone == 0) startPoints = startPoints + point else endPoints = endPoints + point
                showPoiSheet = false
            },
            onDismiss = {
                viewModel.clearPoiResults()
                showPoiSheet = false
            },
        )
    }
}

private val RacingSurfaceDark = Color(0xFF0E1320)

@Composable
private fun ZoneToggle(
    selected: Int,
    startCount: Int,
    endCount: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RacingSurfaceDark)
            .padding(3.dp),
    ) {
        ZoneChip("起点", startCount, RacingStart, selected == 0) { onSelect(0) }
        ZoneChip("终点", endCount, RacingEnd, selected == 1) { onSelect(1) }
    }
}

@Composable
private fun ZoneChip(
    label: String,
    count: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) color.copy(alpha = 0.18f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) color.copy(alpha = 0.7f) else RacingOutline,
                RoundedCornerShape(11.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label ${count}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) color else TextSecondary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveTrackSheet(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    distance: Double,
    creatorName: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RacingCardElevated,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
        ) {
            Text(
                text = "保存赛道",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            RacingTextField(
                value = name,
                onValueChange = onNameChange,
                label = "赛道名称",
                placeholder = "例：云石山冲刺",
            )
            Spacer(Modifier.height(14.dp))
            RacingTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = "描述（可选）",
                placeholder = "一句话介绍这条赛道",
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RacingSurfaceDark)
                    .padding(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("估算距离", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text(
                        FormatUtils.formatDistance(distance),
                        style = MaterialTheme.typography.titleSmall,
                        color = RacingAccent,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("创建者", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text(
                        creatorName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            RacingButton(
                text = "保存赛道",
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoiSearchSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    results: List<PoiResult>,
    onSearch: () -> Unit,
    onPick: (PoiResult) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RacingCardElevated,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "搜索位置",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // 搜索输入
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RacingSurfaceDark)
                    .border(1.dp, RacingOutline, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(RacingPrimary),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "输入关键词，如「云石山 起点」",
                                color = TextTertiary,
                                fontSize = 14.sp,
                            )
                        }
                        inner()
                    },
                )
                if (searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = RacingAccent,
                    )
                } else {
                    TextButton(onClick = onSearch, enabled = query.isNotBlank()) {
                        Text("搜索", color = RacingAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (results.isEmpty()) {
                Text(
                    text = if (searching) "搜索中…" else "搜索后选择位置，会自动加到当前编辑的围栏上",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            } else {
                LazyColumn {
                    items(results, key = { it.name + it.address }) { poi ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(poi) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Place,
                                contentDescription = null,
                                tint = RacingAccent,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    poi.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                )
                                Text(
                                    poi.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                "加到围栏",
                                style = MaterialTheme.typography.labelSmall,
                                color = RacingAccent,
                            )
                        }
                    }
                }
            }
        }
    }
}
