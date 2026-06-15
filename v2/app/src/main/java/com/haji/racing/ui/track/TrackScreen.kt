package com.haji.racing.ui.track

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.domain.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    navController: NavController,
    viewModel: TrackViewModel = hiltViewModel(),
) {
    val tracks by viewModel.allTracks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("赛道") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("track_create") }) {
                Icon(Icons.Default.Add, contentDescription = "创建赛道")
            }
        }
    ) { padding ->
        TrackList(
            tracks = tracks,
            onDelete = { viewModel.deleteTrack(it.uid) },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    onDelete: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tracks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无赛道", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tracks, key = { it.uid }) { track ->
            TrackCard(track = track, onDelete = onDelete)
        }
    }
}

@Composable
private fun TrackCard(
    track: Track,
    onDelete: (Track) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "距离: ${FormatUtils.formatDistance(track.totalDistance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onDelete(track) }) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
