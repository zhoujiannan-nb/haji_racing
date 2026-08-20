package com.haji.racing.ui.track

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.core.common.TimeUtils
import com.haji.racing.domain.model.Track
import com.haji.racing.ui.components.ConfirmDialog
import com.haji.racing.ui.components.EmptyState
import com.haji.racing.ui.navigation.Routes
import com.haji.racing.ui.theme.MedalGold
import com.haji.racing.ui.theme.RacingCard
import com.haji.racing.ui.theme.RacingCardElevated
import com.haji.racing.ui.theme.RacingOutline
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.RacingStart
import com.haji.racing.ui.theme.TextPrimary
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.theme.TextTertiary
@Composable
fun TracksScreen(
    navController: NavController,
    viewModel: TracksViewModel = hiltViewModel(),
) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val statsByUid by viewModel.statsByUid.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Track?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            com.haji.racing.ui.components.ScreenTitle(
                "赛道",
                subtitle = "自定义地图 · ${tracks.size} 条",
            )
            Spacer(Modifier.height(8.dp))

            if (tracks.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Flag,
                    title = "还没有赛道",
                    subtitle = "在地图上画出起点和终点围栏，创建你的第一条跑山赛道",
                    actionLabel = "创建赛道",
                    onAction = { navController.navigate(Routes.TRACK_CREATE) },
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                    items(tracks, key = { it.uid }) { track ->
                        TrackCard(
                            track = track,
                            stats = statsByUid[track.uid],
                            onClick = { navController.navigate(Routes.trackDetail(track.uid)) },
                            onDelete = { pendingDelete = track },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { navController.navigate(Routes.TRACK_CREATE) },
            containerColor = RacingPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "创建赛道")
        }
    }

    pendingDelete?.let { track ->
        ConfirmDialog(
            title = "删除赛道「${track.name}」？",
            message = "该赛道下的所有记录与轨迹也会一并删除，且不可恢复。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                viewModel.delete(track)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun TrackCard(
    track: Track,
    stats: com.haji.racing.domain.model.TrackStats?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(RacingCardElevated, RacingCard)))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(RacingPrimary.copy(alpha = 0.25f), RacingStart.copy(alpha = 0.12f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Flag,
                contentDescription = null,
                tint = RacingPrimary,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = track.description ?: "自定义赛道",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoChip(Icons.Filled.Speed, FormatUtils.formatDistance(track.totalDistance))
                Spacer(Modifier.width(8.dp))
                InfoChip(
                    Icons.Filled.Timeline,
                    if (stats != null && stats.recordCount > 0) "${stats.recordCount} 条记录" else "暂无记录",
                )
                if (stats?.bestTimeMs != null) {
                    Spacer(Modifier.width(8.dp))
                    InfoChip(
                        Icons.Filled.Flag,
                        "最快 ${TimeUtils.formatDurationPrecise(stats.bestTimeMs!!)}",
                        tint = MedalGold,
                    )
                }
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "删除",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    tint: Color = TextSecondary,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}
