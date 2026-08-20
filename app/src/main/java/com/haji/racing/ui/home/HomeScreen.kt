package com.haji.racing.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.domain.model.MODE_FREE
import com.haji.racing.domain.model.MODE_TRACK
import com.haji.racing.domain.model.Track
import com.haji.racing.service.RecordingService
import com.haji.racing.service.RecordingState
import com.haji.racing.ui.components.ConfirmDialog
import com.haji.racing.ui.components.GlowStartButton
import com.haji.racing.ui.components.GpsPill
import com.haji.racing.ui.components.RacingButton
import com.haji.racing.ui.components.RacingCard
import com.haji.racing.ui.components.SectionTitle
import com.haji.racing.ui.navigation.Routes
import com.haji.racing.ui.record.RecordingStarter
import com.haji.racing.ui.theme.RacingAccent
import com.haji.racing.ui.theme.RacingOutline
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.TextPrimary
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val selectedTrack by viewModel.selectedTrack.collectAsStateWithLifecycle()
    val lastGps by viewModel.lastGps.collectAsStateWithLifecycle()
    val gpsReady = lastGps != null

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val notificationPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    var showTrackPicker by remember { mutableStateOf(false) }
    var showPermissionSheet by remember { mutableStateOf(false) }

    val recordingData by RecordingService.recordingData.collectAsStateWithLifecycle()
    val isRecordingActive =
        recordingData.state == RecordingState.WAITING ||
            recordingData.state == RecordingState.RECORDING

    // 首次进入引导授权定位
    LaunchedEffect(locationPermission.status) {
        if (!locationPermission.status.isGranted) showPermissionSheet = true
    }

    val canStart = gpsReady && (mode == 1 || selectedTrack != null)

    fun doStart() {
        // 尝试申请通知权限（不阻塞流程）
        if (!notificationPermission.status.isGranted) notificationPermission.launchPermissionRequest()
        RecordingStarter.start(
            context = context,
            trackUid = if (mode == 0) selectedTrack?.uid else null,
            mode = if (mode == 0) MODE_TRACK else MODE_FREE,
        )
        navController.navigate(Routes.RECORDING_LIVE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        // ===== 头部 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Haji Racing",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                )
                val dateStr = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }
                    .format(Date())
                Text(
                    text = "跑山记录 · $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
            GpsPill(ready = gpsReady, accuracyMeters = lastGps?.accuracy)
        }

        if (isRecordingActive) {
            // ===== 记录进行中 =====
            Spacer(Modifier.weight(1f))
            Text(
                text = "有一条记录正在进行中",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            RacingButton(
                text = "返回实时记录",
                onClick = { navController.navigate(Routes.RECORDING_LIVE) },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Route,
            )
            Spacer(Modifier.weight(1f))
        } else {
            // ===== 模式选择 =====
            ModeCard(mode = mode, onModeChange = viewModel::setMode)
            Spacer(Modifier.height(16.dp))

            if (mode == 0) {
                TrackSelectCard(
                    selected = selectedTrack,
                    onClick = { showTrackPicker = true },
                    onCreate = { navController.navigate(Routes.TRACK_CREATE) },
                )
                Spacer(Modifier.height(28.dp))
            } else {
                Spacer(Modifier.height(40.dp))
            }

            // ===== 大启动按钮 =====
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlowStartButton(
                    onClick = {
                        if (!locationPermission.status.isGranted) showPermissionSheet = true
                        else doStart()
                    },
                    enabled = canStart,
                    icon = if (mode == 0) Icons.Filled.AddRoad else Icons.Filled.Explore,
                )
            }
            Spacer(Modifier.height(16.dp))

            Text(
                text = when {
                    !gpsReady -> "正在获取 GPS 信号，请保持手机电量充足"
                    mode == 0 && selectedTrack == null -> "请先选择一条赛道"
                    mode == 0 -> "点击开始：进入起点区域并加速至 15 km/h 自动计时"
                    else -> "点击开始，行驶中可随时手动停止"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (canStart) TextSecondary else TextTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
        }
    }

    // ===== 赛道选择 BottomSheet =====
    if (showTrackPicker) {
        TrackPickerSheet(
            tracks = allTracks,
            selected = selectedTrack,
            onSelect = {
                viewModel.selectTrack(it)
                showTrackPicker = false
            },
            onDismiss = { showTrackPicker = false },
            onCreate = {
                showTrackPicker = false
                navController.navigate(Routes.TRACK_CREATE)
            },
        )
    }

    // ===== 权限引导 =====
    if (showPermissionSheet && !locationPermission.status.isGranted) {
        val deniedPermanently =
            locationPermission.status is PermissionStatus.Denied &&
                !locationPermission.status.shouldShowRationale
        ConfirmDialog(
            title = "需要定位权限",
            message = if (deniedPermanently) {
                "定位权限已被永久拒绝，请在系统设置中开启后使用记录功能。位置数据只保存在本机。"
            } else {
                "记录需要持续获取 GPS 位置以计算轨迹、距离与圈速。数据只保存在本机，不会上传。"
            },
            confirmText = if (deniedPermanently) "去设置" else "允许",
            onConfirm = {
                if (deniedPermanently) openAppSettings(context)
                else locationPermission.launchPermissionRequest()
                showPermissionSheet = false
            },
            onDismiss = { showPermissionSheet = false },
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@Composable
private fun ModeCard(mode: Int, onModeChange: (Int) -> Unit) {
    RacingCard {
        Text(
            text = "记录模式",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(12.dp))
        com.haji.racing.ui.components.ModeSegmented(
            options = listOf("赛道跟跑", "自由跑"),
            selected = mode,
            onSelect = onModeChange,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (mode == 0) {
                "沿自定义赛道跟跑：起点区域自动计时，终点区域自动完成"
            } else {
                "无需赛道，自由骑行 / 驾驶，手动开始与停止"
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
    }
}

@Composable
private fun TrackSelectCard(
    selected: Track?,
    onClick: () -> Unit,
    onCreate: () -> Unit,
) {
    if (selected == null) {
        RacingCard(onClick = onCreate) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = RacingPrimary,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "创建你的第一条赛道",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "在地图上画起点 / 终点围栏，立即可用",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextTertiary,
                )
            }
        }
    } else {
        RacingCard(onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    RacingPrimary.copy(alpha = 0.25f),
                                    RacingAccent.copy(alpha = 0.12f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        tint = RacingPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = selected.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = "${FormatUtils.formatDistance(selected.totalDistance)} · ${
                            selected.description ?: "自定义赛道"
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = null,
                    tint = RacingAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackPickerSheet(
    tracks: List<Track>,
    selected: Track?,
    onSelect: (Track) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.haji.racing.ui.theme.RacingCardElevated,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "选择赛道",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (tracks.isEmpty()) {
                Text(
                    text = "还没有赛道，先去创建一条吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                tracks.forEach { track ->
                    val isSelected = selected?.uid == track.uid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) RacingPrimary.copy(alpha = 0.12f)
                                else androidx.compose.ui.graphics.Color.Transparent,
                            )
                            .padding(vertical = 10.dp)
                            .then(
                                Modifier.clickable {
                                    onSelect(track)
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = if (isSelected) RacingPrimary else TextTertiary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                text = FormatUtils.formatDistance(track.totalDistance),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            RacingButton(
                text = "新建赛道",
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                variant = com.haji.racing.ui.components.ButtonVariant.Outline,
            )
        }
    }
}
