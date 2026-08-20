package com.haji.racing.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.amap.api.maps.AMap
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.Polyline
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.core.common.TimeUtils
import com.haji.racing.domain.model.MODE_FREE
import com.haji.racing.domain.model.MODE_TRACK
import com.haji.racing.service.RecordingState
import com.haji.racing.service.RecordingService
import com.haji.racing.ui.components.ConfirmDialog
import com.haji.racing.ui.components.GpsPill
import com.haji.racing.ui.components.RacingButton
import com.haji.racing.ui.components.SpeedGauge
import com.haji.racing.ui.components.StatusChip
import com.haji.racing.ui.components.ButtonVariant
import com.haji.racing.ui.navigation.Routes
import com.haji.racing.ui.theme.RacingCard
import com.haji.racing.ui.theme.RacingEnd
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.RacingStart
import com.haji.racing.ui.theme.RacingWarn
import com.haji.racing.ui.theme.TextPrimary
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.theme.TextTertiary
import com.haji.racing.ui.theme.TimerTextStyle
import com.haji.racing.ui.track.AmapView
import com.haji.racing.ui.track.MapFences
import kotlinx.coroutines.delay

@Composable
fun RecordingLiveScreen(
    navController: NavController,
    viewModel: RecordingLiveViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val data by viewModel.data.collectAsStateWithLifecycle()
    val route by viewModel.liveRoute.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val track by viewModel.track.collectAsStateWithLifecycle()
    val lastGps by viewModel.lastGps.collectAsStateWithLifecycle()

    // 10Hz 计时刷新
    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(data.state, data.timingStartEpoch) {
        while (true) {
            val d = RecordingService.recordingData.value
            elapsedMs = if (d.state == RecordingState.RECORDING) {
                System.currentTimeMillis() - d.timingStartEpoch
            } else {
                d.elapsedMs
            }
            delay(100)
        }
    }

    var showStopDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val isFree = data.mode == MODE_FREE
    val isActive =
        data.state == RecordingState.WAITING || data.state == RecordingState.RECORDING

    fun stopRecording() {
        RecordingStarter.stop(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            // ===== 顶部 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    if (isActive) showExitDialog = true
                    else navController.popBackStack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "退出",
                        tint = TextPrimary,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track?.name ?: "自由跑",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = if (isFree) "自由跑 · 手动停止" else "赛道跟跑 · 终点自动完成",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
                GpsPill(
                    ready = data.gpsReady || lastGps != null,
                    accuracyMeters = data.accuracy.takeIf { it < Float.MAX_VALUE },
                )
            }

            Spacer(Modifier.height(10.dp))

            // ===== 状态行 =====
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    text = when (data.state) {
                        RecordingState.WAITING -> "等待起跑"
                        RecordingState.RECORDING -> "记录中"
                        else -> "已结束"
                    },
                    color = when (data.state) {
                        RecordingState.WAITING -> RacingWarn
                        RecordingState.RECORDING -> RacingEnd
                        else -> TextTertiary
                    },
                    icon = if (data.state == RecordingState.RECORDING) Icons.Filled.PlayArrow else Icons.Filled.MyLocation,
                    active = isActive,
                )
                Spacer(Modifier.width(8.dp))
                if (!isFree) {
                    StatusChip(
                        text = if (data.inStartZone) "起点区域内" else "不在起点区域",
                        color = RacingStart,
                        active = data.inStartZone,
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(
                        text = if (data.inEndZone) "进入终点区域" else "终点区域",
                        color = RacingEnd,
                        active = data.inEndZone,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===== 大计时器 =====
            Text(
                text = TimeUtils.formatDurationPrecise(elapsedMs),
                style = TimerTextStyle,
                color = when (data.state) {
                    RecordingState.RECORDING -> TextPrimary
                    RecordingState.WAITING -> RacingWarn
                    else -> TextSecondary
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            // ===== 速度表 =====
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SpeedGauge(speedKmh = (data.currentSpeed * 3.6).toFloat(), diameter = 210.dp)
            }

            Spacer(Modifier.height(14.dp))

            // ===== 实时统计 =====
            Row {
                LiveStat("距离", FormatUtils.formatDistance(data.currentDistance), Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                LiveStat("最高", FormatUtils.formatSpeed(data.maxSpeed), Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                LiveStat(
                    "GPS精度",
                    if (data.accuracy < Float.MAX_VALUE) "${data.accuracy.toInt()}m" else "--",
                    Modifier.weight(1f),
                )
            }

            if (!isFree && data.distanceToStart != null && data.state == RecordingState.WAITING) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "距起点围栏 ${FormatUtils.formatDistance(data.distanceToStart!!)} · 进入区域并加速至 ${RecordingService.START_TRIGGER_SPEED_KMH.toInt()} km/h 自动计时",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            Spacer(Modifier.height(14.dp))

            // ===== 实时地图 =====
            LiveMap(
                track = track,
                isFree = isFree,
                route = route,
            )

            Spacer(Modifier.height(14.dp))

            // ===== 停止按钮 =====
            if (isActive) {
                RacingButton(
                    text = if (data.state == RecordingState.RECORDING) "停止记录" else "结束记录",
                    onClick = { showStopDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    icon = Icons.Filled.Stop,
                    variant = ButtonVariant.Danger,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ===== 停止确认 =====
    if (showStopDialog) {
        ConfirmDialog(
            title = if (data.state == RecordingState.RECORDING) "停止记录？" else "结束记录？",
            message = when {
                data.state == RecordingState.WAITING -> "尚未开始计时，结束将不保存记录。"
                isFree -> "将保存本次自由跑记录。"
                else -> "未到达终点，记录将标记为「未完成」。"
            },
            confirmText = "确定",
            onConfirm = {
                showStopDialog = false
                stopRecording()
            },
            onDismiss = { showStopDialog = false },
        )
    }

    // ===== 退出确认 =====
    if (showExitDialog) {
        ConfirmDialog(
            title = "退出页面？",
            message = "记录会在后台继续，通知栏可查看实时状态。之后可从首页返回。",
            confirmText = "退出",
            onConfirm = {
                showExitDialog = false
                navController.popBackStack()
            },
            onDismiss = { showExitDialog = false },
        )
    }

    // ===== 结果卡 =====
    AnimatedVisibility(
        visible = result != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val r = result!!
        ResultOverlay(
            status = r.status,
            durationMs = r.durationMs,
            distance = r.totalDistance,
            avgSpeed = r.avgSpeed,
            maxSpeed = r.maxSpeed,
            isFree = isFree,
            onViewDetail = {
                navController.navigate(Routes.recordDetail(r.uid))
            },
            onHome = { navController.popBackStack() },
        )
    }

    // ===== 未计时结束提示 =====
    AnimatedVisibility(
        visible = result == null && data.state == RecordingState.FINISHED,
        enter = fadeIn(),
    ) {
        ResultOverlay(
            status = "discarded",
            durationMs = 0L,
            distance = data.currentDistance,
            avgSpeed = 0.0,
            maxSpeed = data.maxSpeed,
            isFree = isFree,
            discarded = true,
            onViewDetail = {},
            onHome = { navController.popBackStack() },
        )
    }
}

@Composable
private fun LiveStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RacingCard)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
    }
}

@Composable
private fun LiveMap(
    track: com.haji.racing.domain.model.Track?,
    isFree: Boolean,
    route: List<com.haji.racing.core.gps.GpsPoint>,
) {
    var mapReady by remember { mutableStateOf(false) }
    var liveLine by remember { mutableStateOf<Polyline?>(null) }
    var liveMarker by remember { mutableStateOf<Marker?>(null) }
    var cameraSet by remember { mutableStateOf(false) }

    LaunchedEffect(track, isFree, mapReady) {
        if (!mapReady) return@LaunchedEffect
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(com.haji.racing.ui.theme.RacingCard),
    ) {
        AmapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { aMap ->
                if (mapReady) return@AmapView
                mapReady = true
                if (track != null) {
                    MapFences.drawTrackFences(aMap, track)
                    MapFences.fitToFences(aMap, track)
                } else {
                    MapFences.moveTo(aMap, 30.2421, 120.1536, 14f)
                }
                val (line, marker) = MapFences.attachLiveRoute(aMap)
                liveLine = line
                liveMarker = marker
            },
        )
    }

    LaunchedEffect(route, liveLine) {
        if (route.isNotEmpty()) {
            MapFences.updateLiveRoute(liveLine, liveMarker, route)
            if (isFree && !cameraSet) {
                cameraSet = true
                // 自由跑：跟随第一个点
            }
        }
    }
}

@Composable
private fun ResultOverlay(
    status: String,
    durationMs: Long,
    distance: Double,
    avgSpeed: Double,
    maxSpeed: Double,
    isFree: Boolean,
    discarded: Boolean = false,
    onViewDetail: () -> Unit,
    onHome: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xE60A0D13)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Text(
                text = when {
                    discarded -> "未开始计时"
                    status == "completed" -> "🏁 完成！" else -> "记录已保存"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = if (status == "completed") RacingStart else TextPrimary,
            )
            Spacer(Modifier.height(8.dp))

            if (discarded) {
                Text(
                    text = if (isFree) "本次记录未保存。" else "尚未进入起点区域并达到 15 km/h，本次未保存。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            } else {
                Text(
                    text = TimeUtils.formatDurationPrecise(durationMs),
                    style = TimerTextStyle,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (status == "incomplete") "中途停止 · 标记为未完成" else "完成记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status == "incomplete") RacingWarn else TextSecondary,
                )
            }

            Spacer(Modifier.height(20.dp))

            if (!discarded) {
                Row {
                    LiveStat("距离", FormatUtils.formatDistance(distance), Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    LiveStat("均速", FormatUtils.formatSpeed(avgSpeed), Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    LiveStat("最高", FormatUtils.formatSpeed(maxSpeed), Modifier.weight(1f))
                }
            }

            Spacer(Modifier.weight(1f))

            if (!discarded) {
                RacingButton(
                    text = "查看轨迹详情",
                    onClick = onViewDetail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    icon = Icons.Filled.Flag,
                )
                Spacer(Modifier.height(10.dp))
            }
            RacingButton(
                text = "返回首页",
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                variant = ButtonVariant.Ghost,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
