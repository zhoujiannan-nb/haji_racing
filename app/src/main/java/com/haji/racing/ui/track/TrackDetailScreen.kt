package com.haji.racing.ui.track

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.amap.api.maps.AMap
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.core.common.TimeUtils
import com.haji.racing.domain.model.MODE_TRACK
import com.haji.racing.domain.model.durationMs
import com.haji.racing.ui.components.ConfirmDialog
import com.haji.racing.ui.components.RacingButton
import com.haji.racing.ui.components.RacingCard
import com.haji.racing.ui.components.ScreenTitle
import com.haji.racing.ui.components.SectionTitle
import com.haji.racing.ui.components.StatTile
import com.haji.racing.ui.navigation.Routes
import com.haji.racing.ui.record.RecordingStarter
import com.haji.racing.ui.theme.MedalBronze
import com.haji.racing.ui.theme.MedalGold
import com.haji.racing.ui.theme.MedalSilver
import com.haji.racing.ui.theme.RacingOutline
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.RacingStart
import com.haji.racing.ui.theme.TextPrimary
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrackDetailScreen(
    navController: NavController,
    viewModel: TrackDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val track by viewModel.track.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var mapRef by remember { mutableStateOf<AMap?>(null) }
    var mapReady by remember { mutableStateOf(false) }

    if (track == null) {
        // 加载中
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = TextSecondary)
            }
        }
        return
    }

    val current = track!!

    fun startRecording() {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
            return
        }
        RecordingStarter.start(context, current.uid, MODE_TRACK)
        navController.navigate(Routes.RECORDING_LIVE)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
    ) {
        // ===== 头部 =====
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
                Column(Modifier.weight(1f)) {
                    Text(
                        text = current.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    current.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = TextTertiary,
                    )
                }
            }
        }

        // ===== 地图 =====
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(com.haji.racing.ui.theme.RacingCard),
            ) {
                AmapView(
                    modifier = Modifier.fillMaxSize(),
                    onMapReady = { aMap ->
                        if (mapReady) return@AmapView
                        mapReady = true
                        mapRef = aMap
                        MapFences.drawTrackFences(aMap, current)
                        MapFences.fitToFences(aMap, current)
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ===== 统计 =====
        item {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "赛道长度",
                    value = FormatUtils.formatDistance(current.totalDistance),
                    icon = Icons.Filled.SwapHoriz,
                    modifier = Modifier.weight(1f),
                    accent = RacingStart,
                )
                StatTile(
                    label = "记录数",
                    value = "${stats?.recordCount ?: recordings.size}",
                    icon = Icons.Filled.Flag,
                    modifier = Modifier.weight(1f),
                    accent = RacingPrimary,
                )
                StatTile(
                    label = "最快成绩",
                    value = stats?.bestTimeMs?.let { TimeUtils.formatDurationPrecise(it) } ?: "--:--",
                    icon = Icons.Filled.Timer,
                    modifier = Modifier.weight(1f),
                    accent = MedalGold,
                    valueColor = MedalGold,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ===== 排行榜 =====
        item {
            SectionTitle("排行榜 TOP 5")
            Spacer(Modifier.height(10.dp))
            if (leaderboard.isEmpty()) {
                RacingCard {
                    Text(
                        "还没有完成记录，来跑第一条吧！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            } else {
                leaderboard.forEachIndexed { index, rec ->
                    LeaderboardRow(
                        rank = index + 1,
                        timeMs = rec.durationMs,
                        distance = rec.totalDistance,
                        date = rec.startTime,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ===== 最近记录 =====
        if (recordings.isNotEmpty()) {
            item {
                SectionTitle("最近记录")
                Spacer(Modifier.height(10.dp))
                recordings.take(3).forEach { rec ->
                    RecentRecordRow(
                        timeMs = rec.durationMs,
                        date = rec.startTime,
                        distance = rec.totalDistance,
                        completed = rec.status == "completed",
                        onClick = { navController.navigate(Routes.recordDetail(rec.uid)) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ===== 开始 CTA =====
        item {
            RacingButton(
                text = "开始跟跑",
                onClick = { startRecording() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                icon = Icons.Filled.PlayArrow,
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "删除赛道「${current.name}」？",
            message = "该赛道下的所有记录与轨迹也会一并删除，且不可恢复。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
                navController.popBackStack()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    timeMs: Long,
    distance: Double,
    date: Long,
) {
    val rankColor = when (rank) {
        1 -> MedalGold
        2 -> MedalSilver
        3 -> MedalBronze
        else -> TextTertiary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(com.haji.racing.ui.theme.RacingCard)
            .border(1.dp, RacingOutline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(rankColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$rank",
                color = rankColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "第 ${rank} 名",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Text(
                text = SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(date)),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        Text(
            text = TimeUtils.formatDurationPrecise(timeMs),
            style = MaterialTheme.typography.titleMedium,
            color = rankColor,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = FormatUtils.formatDistance(distance),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun RecentRecordRow(
    timeMs: Long,
    date: Long,
    distance: Double,
    completed: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(com.haji.racing.ui.theme.RacingCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Flag,
            contentDescription = null,
            tint = if (completed) RacingStart else TextTertiary,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (completed) "完成" else "未完成",
                style = MaterialTheme.typography.labelMedium,
                color = if (completed) RacingStart else TextTertiary,
            )
            Text(
                text = SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(date)),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        Text(
            text = TimeUtils.formatDurationPrecise(timeMs),
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = FormatUtils.formatDistance(distance),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}
