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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Flag
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
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.core.common.TimeUtils
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.model.durationMs
import com.haji.racing.domain.model.isCompleted
import com.haji.racing.ui.components.ConfirmDialog
import com.haji.racing.ui.components.EmptyState
import com.haji.racing.ui.components.RecordingStatusChip
import com.haji.racing.ui.components.ScreenTitle
import com.haji.racing.ui.navigation.Routes
import com.haji.racing.ui.theme.RacingCard
import com.haji.racing.ui.theme.RacingOutline
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.TextPrimary
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordsScreen(
    navController: NavController,
    viewModel: RecordsViewModel = hiltViewModel(),
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<RecordItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        ScreenTitle("轨迹", subtitle = "全部记录 · ${records.size} 条")
        Spacer(Modifier.height(8.dp))

        if (records.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                title = "还没有轨迹",
                subtitle = "完成一次跟跑或自由跑后，轨迹会出现在这里",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(records, key = { it.recording.uid }) { item ->
                    RecordCard(
                        item = item,
                        onClick = {
                            navController.navigate(Routes.recordDetail(item.recording.uid))
                        },
                        onDelete = { pendingDelete = item },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "删除这条轨迹？",
            message = "将同时删除轨迹点数据，且不可恢复。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                viewModel.delete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun RecordCard(
    item: RecordItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val recording = item.recording
    val isFree = recording.trackUid.isEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(com.haji.racing.ui.theme.RacingCardElevated, RacingCard),
                ),
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧：赛道图标
        Box(
            modifier = Modifier
                .padding(end = 14.dp)
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isFree) RacingPrimary.copy(alpha = 0.14f)
                    else com.haji.racing.ui.theme.RacingStart.copy(alpha = 0.12f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isFree) Icons.AutoMirrored.Filled.DirectionsBike else Icons.Filled.Flag,
                contentDescription = null,
                tint = if (isFree) RacingPrimary else com.haji.racing.ui.theme.RacingStart,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.trackName ?: "自由跑",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                RecordingStatusChip(recording.status)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = remember(recording.startTime) {
                    SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(recording.startTime))
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${FormatUtils.formatDistance(recording.totalDistance)} · " +
                    "均速 ${FormatUtils.formatSpeed(recording.avgSpeed)} · " +
                    "最高 ${FormatUtils.formatSpeed(recording.maxSpeed)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = TimeUtils.formatDuration(recording.durationMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
