package com.haji.racing.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.core.common.TimeUtils
import com.haji.racing.domain.model.Recording
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val recordings by viewModel.allRecordings.collectAsState()
    val selectedForCompare by viewModel.selectedForCompare.collectAsState()
    val compareRecordings by viewModel.compareRecordings.collectAsState()
    val selectedRecording by viewModel.selectedRecording.collectAsState()

    var isCompareMode by remember { mutableStateOf(false) }
    var showCompare by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的轨迹") },
                actions = {
                    IconButton(onClick = { isCompareMode = !isCompareMode; viewModel.clearCompare() }) {
                        Icon(
                            imageVector = if (isCompareMode) Icons.Default.Close else Icons.Default.Compare,
                            contentDescription = if (isCompareMode) "退出对比" else "对比模式"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (isCompareMode && selectedForCompare.size == 2) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.startCompare()
                        showCompare = true
                    },
                    icon = { Icon(Icons.Default.Compare, "对比") },
                    text = { Text("开始对比") },
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            selectedRecording?.let { recording ->
                RecordingDetailCard(recording)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (recordings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("暂无轨迹记录", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(recordings, key = { it.uid }) { recording ->
                        RecordingListItem(
                            recording = recording,
                            isCompareMode = isCompareMode,
                            isSelected = selectedForCompare.contains(recording.uid),
                            onClick = {
                                if (isCompareMode) {
                                    viewModel.toggleCompareSelection(recording.uid)
                                } else {
                                    viewModel.selectRecording(recording.uid)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCompare && compareRecordings.size == 2) {
        CompareDialog(
            rec1 = compareRecordings[0],
            rec2 = compareRecordings[1],
            onDismiss = { showCompare = false },
        )
    }
}

@Composable
private fun RecordingListItem(
    recording: Recording,
    isCompareMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = if (isCompareMode && isSelected) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = dateFormat.format(Date(recording.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${FormatUtils.formatDistance(recording.totalDistance)} | ${TimeUtils.formatDuration(recording.endTime - recording.startTime)}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "均速 ${FormatUtils.formatSpeed(recording.avgSpeed)} | 最大 ${FormatUtils.formatSpeed(recording.maxSpeed)} | 平均 ${FormatUtils.formatG(recording.avgG.toFloat())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isCompareMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            }
        }
    }
}

@Composable
private fun RecordingDetailCard(recording: Recording) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("轨迹详情", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatColumn("距离", FormatUtils.formatDistance(recording.totalDistance))
                StatColumn("时间", TimeUtils.formatDuration(recording.endTime - recording.startTime))
                StatColumn("均速", FormatUtils.formatSpeed(recording.avgSpeed))
                StatColumn("最大速度", FormatUtils.formatSpeed(recording.maxSpeed))
                StatColumn("平均G", FormatUtils.formatG(recording.avgG.toFloat()))
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompareDialog(
    rec1: Recording,
    rec2: Recording,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("轨迹对比") },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CompareStat("距离", FormatUtils.formatDistance(rec1.totalDistance), FormatUtils.formatDistance(rec2.totalDistance))
                    CompareStat("时间", TimeUtils.formatDuration(rec1.endTime - rec1.startTime), TimeUtils.formatDuration(rec2.endTime - rec2.startTime))
                    CompareStat("均速", FormatUtils.formatSpeed(rec1.avgSpeed), FormatUtils.formatSpeed(rec2.avgSpeed))
                    CompareStat("最大速度", FormatUtils.formatSpeed(rec1.maxSpeed), FormatUtils.formatSpeed(rec2.maxSpeed))
                }
                Spacer(modifier = Modifier.height(16.dp))
                SpeedDistanceChart(rec1 = rec1, rec2 = rec2)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun CompareStat(label: String, val1: String, val2: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(val1, style = MaterialTheme.typography.bodySmall, color = Color(0xFF2196F3))
        Text(val2, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE91E63))
    }
}

@Composable
private fun SpeedDistanceChart(rec1: Recording, rec2: Recording) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        val width = size.width
        val height = size.height
        val maxDist = maxOf(rec1.totalDistance, rec2.totalDistance).toFloat().coerceAtLeast(1f)
        val maxSpd = maxOf(rec1.maxSpeed, rec2.maxSpeed).toFloat().coerceAtLeast(1f)

        drawLine(Color(0xFF2196F3), Offset(0f, height), Offset(width, height * (1f - (rec1.avgSpeed / maxSpd).toFloat())), strokeWidth = 2f)
        drawLine(Color(0xFFE91E63), Offset(0f, height), Offset(width, height * (1f - (rec2.avgSpeed / maxSpd).toFloat())), strokeWidth = 2f)
    }
}
