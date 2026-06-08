package com.haji.racing.ui.recording

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.core.common.TimeUtils
import com.haji.racing.domain.model.Recording
import com.haji.racing.domain.model.Track
import com.haji.racing.service.RecordingService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    navController: NavController,
    viewModel: RecordingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val tracks by viewModel.tracks.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val selectedRefUid by viewModel.selectedRefRecordingUid.collectAsState()
    val refRecordings by viewModel.refRecordings.collectAsState()
    val recordingData by com.haji.racing.service.RecordingService.recordingData.collectAsState()
    var showTrackPicker by remember { mutableStateOf(false) }
    var showModePicker by remember { mutableStateOf(false) }
    var isCruiseMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    LaunchedEffect(recordingData.state) {
        if (recordingData.state == com.haji.racing.service.RecordingState.FINISHED) {
            isRecording = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isCruiseMode) "巡游模式" else "赛道记录") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isRecording) {
                RecordingStatusView(recordingData = recordingData)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { stopRecording(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("停止记录")
                }
            } else {
                ModeSelector(isCruiseMode = isCruiseMode, onModeChange = { isCruiseMode = it })
                Spacer(modifier = Modifier.height(16.dp))

                if (!isCruiseMode) {
                    TrackSelector(
                        selectedTrack = selectedTrack,
                        onClick = { showTrackPicker = true },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RefRecordingSelector(
                        recordings = refRecordings,
                        selectedUid = selectedRefUid,
                        onSelect = { viewModel.selectRefRecording(it) },
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Button(
                    onClick = {
                        startRecording(context, selectedTrack, isCruiseMode, selectedRefUid)
                        isRecording = true
                    },
                    enabled = isCruiseMode || selectedTrack != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始记录", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (showTrackPicker) {
        TrackPickerDialog(
            tracks = tracks,
            onSelect = { track ->
                viewModel.selectTrack(track.uid)
                showTrackPicker = false
            },
            onDismiss = { showTrackPicker = false },
        )
    }
}

@Composable
private fun ModeSelector(isCruiseMode: Boolean, onModeChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = !isCruiseMode,
            onClick = { onModeChange(false) },
            label = { Text("赛道模式") },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = isCruiseMode,
            onClick = { onModeChange(true) },
            label = { Text("巡游模式") },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TrackSelector(selectedTrack: Track?, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedTrack?.name ?: "选择赛道",
                style = MaterialTheme.typography.bodyLarge,
                color = if (selectedTrack != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = ">",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RefRecordingSelector(
    recordings: List<Recording>,
    selectedUid: String?,
    onSelect: (String?) -> Unit,
) {
    if (recordings.isEmpty()) return

    Text("对比参考轨迹", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedUid == null,
            onClick = { onSelect(null) },
            label = { Text("无") },
        )
        recordings.take(3).forEach { rec ->
            val label = TimeUtils.formatDuration(rec.endTime - rec.startTime)
            FilterChip(
                selected = selectedUid == rec.uid,
                onClick = { onSelect(rec.uid) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun RecordingStatusView(recordingData: com.haji.racing.service.RecordingData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = TimeUtils.formatDurationPrecise(recordingData.elapsedMs),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem("距离", FormatUtils.formatDistance(recordingData.currentDistance))
                StatItem("速度", FormatUtils.formatSpeed(recordingData.currentSpeed))
                StatItem("G值", FormatUtils.formatG(recordingData.currentG))
            }

            if (recordingData.timeDiffMs != 0L) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "对比: ${FormatUtils.formatTimeDiff(recordingData.timeDiffMs)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (recordingData.timeDiffMs < 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun TrackPickerDialog(
    tracks: List<Track>,
    onSelect: (Track) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择赛道") },
        text = {
            Column {
                tracks.forEach { track ->
                    TextButton(
                        onClick = { onSelect(track) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(track.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun startRecording(context: Context, track: Track?, isCruise: Boolean, refUid: String?) {
    val intent = Intent(context, RecordingService::class.java).apply {
        action = RecordingService.ACTION_START
        putExtra(RecordingService.EXTRA_TRACK_UID, track?.uid ?: "cruise")
        putExtra(RecordingService.EXTRA_MODE, if (isCruise) "cruise" else "track")
        refUid?.let { putExtra(RecordingService.EXTRA_REF_RECORDING_UID, it) }
    }
    context.startForegroundService(intent)
}

private fun stopRecording(context: Context) {
    val intent = Intent(context, RecordingService::class.java).apply {
        action = RecordingService.ACTION_STOP
    }
    context.startService(intent)
}
