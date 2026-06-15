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
    val recordingData by RecordingService.recordingData.collectAsState()
    var showTrackPicker by remember { mutableStateOf(false) }
    var isFreeMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    LaunchedEffect(recordingData.state) {
        if (recordingData.state == com.haji.racing.service.RecordingState.FINISHED) {
            isRecording = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isFreeMode) "自由跑" else "赛道记录") })
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
                ModeSelector(isFreeMode = isFreeMode, onModeChange = { isFreeMode = it })
                Spacer(modifier = Modifier.height(16.dp))

                if (!isFreeMode) {
                    TrackSelector(
                        selectedTrack = selectedTrack,
                        onClick = { showTrackPicker = true },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        startRecording(context, selectedTrack, isFreeMode)
                        isRecording = true
                    },
                    enabled = isFreeMode || selectedTrack != null,
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
private fun ModeSelector(isFreeMode: Boolean, onModeChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = !isFreeMode,
            onClick = { onModeChange(false) },
            label = { Text("赛道模式") },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = isFreeMode,
            onClick = { onModeChange(true) },
            label = { Text("自由跑") },
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
                StatItem("最大", FormatUtils.formatSpeed(recordingData.maxSpeed))
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

private fun startRecording(context: Context, track: Track?, isFree: Boolean) {
    val intent = Intent(context, RecordingService::class.java).apply {
        action = RecordingService.ACTION_START
        putExtra(RecordingService.EXTRA_TRACK_UID, track?.uid ?: "free")
        putExtra(RecordingService.EXTRA_MODE, if (isFree) "free" else "track")
    }
    context.startForegroundService(intent)
}

private fun stopRecording(context: Context) {
    val intent = Intent(context, RecordingService::class.java).apply {
        action = RecordingService.ACTION_STOP
    }
    context.startService(intent)
}
