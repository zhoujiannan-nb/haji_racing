package com.haji.racing.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.amap.api.maps.AMap
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.ui.track.AmapView
import com.haji.racing.core.common.TimeUtils
import com.haji.racing.domain.model.durationMs
import com.haji.racing.ui.components.ConfirmDialog
import com.haji.racing.ui.components.RacingButton
import com.haji.racing.ui.components.RacingCard
import com.haji.racing.ui.components.RecordingStatusChip
import com.haji.racing.ui.components.SectionTitle
import com.haji.racing.ui.components.SpeedChart
import com.haji.racing.ui.components.StatTile
import com.haji.racing.ui.components.ButtonVariant
import com.haji.racing.ui.track.MapFences
import com.haji.racing.ui.theme.RacingEnd
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.RacingPrimaryLight
import com.haji.racing.ui.theme.TextPrimary
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordDetailScreen(
    navController: NavController,
    viewModel: RecordDetailViewModel = hiltViewModel(),
) {
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    val speedSamples by viewModel.speedSamples.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val rec = recording ?: run {
        // 未加载：稍后由 flow 触发重组
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = TextSecondary)
            }
        }
        return
    }

    val isFree = rec.trackUid.isEmpty()
    val maxKmh = (rec.maxSpeed * 3.6).toFloat().coerceAtLeast(30f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
    ) {
        // ===== 顶部导航 =====
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "轨迹详情",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = TextTertiary,
                    )
                }
            }
        }

        // ===== Hero =====
        item {
            RacingCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = TimeUtils.formatDuration(rec.durationMs),
                            style = MaterialTheme.typography.displayMedium,
                            color = TextPrimary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault())
                                .format(Date(rec.startTime)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    RecordingStatusChip(rec.status)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ===== 统计 =====
        item {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "距离",
                    value = FormatUtils.formatDistance(rec.totalDistance),
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    modifier = Modifier.weight(1f),
                    accent = RacingPrimary,
                )
                StatTile(
                    label = "平均速度",
                    value = FormatUtils.formatSpeed(rec.avgSpeed),
                    icon = Icons.Filled.Speed,
                    modifier = Modifier.weight(1f),
                    accent = com.haji.racing.ui.theme.RacingAccent,
                )
                StatTile(
                    label = "最高速度",
                    value = FormatUtils.formatSpeed(rec.maxSpeed),
                    icon = Icons.Filled.SportsMotorsports,
                    modifier = Modifier.weight(1f),
                    accent = RacingEnd,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ===== 轨迹地图 =====
        item {
            SectionTitle("轨迹回放")
            Spacer(Modifier.height(10.dp))
            TrajectoryMap(points = rec.points.map { Triple(it.lat, it.lng, it.speed) })
            Spacer(Modifier.height(16.dp))
        }

        // ===== 速度曲线 =====
        if (speedSamples.isNotEmpty()) {
            item {
                SectionTitle("速度曲线")
                Spacer(Modifier.height(10.dp))
                RacingCard {
                    SpeedChart(samples = speedSamples, maxSpeed = maxKmh)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "删除这条轨迹？",
            message = "将同时删除轨迹点数据，且不可恢复。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete { navController.popBackStack() }
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

/** 轨迹地图（按速度染色） */
@Composable
private fun TrajectoryMap(points: List<Triple<Double, Double, Double>>) {
    var mapInitialized by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(com.haji.racing.ui.theme.RacingCard),
    ) {
        AmapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { aMap ->
                if (mapInitialized) return@AmapView
                mapInitialized = true
                if (points.isNotEmpty()) drawTrajectory(aMap, points)
            },
        )
        if (points.isEmpty()) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("无轨迹点", color = TextTertiary)
            }
        }
    }
}

private fun drawTrajectory(aMap: AMap, points: List<Triple<Double, Double, Double>>) {
    if (points.isEmpty()) return
    val maxSpeed = points.maxOf { it.third }
    MapFences.drawTrajectory(
        aMap = aMap,
        points = points,
        maxSpeed = maxSpeed.coerceAtLeast(1.0),
        segmentCount = 28,
    )
    MapFences.fitToPoints(
        aMap = aMap,
        points = points.map { com.haji.racing.domain.model.FencePoint(it.first, it.second) },
    )
    // 起终点标记
    aMap.addMarker(
        com.amap.api.maps.model.MarkerOptions()
            .position(com.amap.api.maps.model.LatLng(points.first().first, points.first().second))
            .icon(com.amap.api.maps.model.BitmapDescriptorFactory.defaultMarker(
                com.amap.api.maps.model.BitmapDescriptorFactory.HUE_GREEN,
            )),
    )
    aMap.addMarker(
        com.amap.api.maps.model.MarkerOptions()
            .position(com.amap.api.maps.model.LatLng(points.last().first, points.last().second))
            .icon(com.amap.api.maps.model.BitmapDescriptorFactory.defaultMarker(
                com.amap.api.maps.model.BitmapDescriptorFactory.HUE_RED,
            )),
    )
}
