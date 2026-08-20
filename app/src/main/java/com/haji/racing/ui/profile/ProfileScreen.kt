package com.haji.racing.ui.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.core.common.TimeUtils
import com.haji.racing.ui.components.ConfirmDialog
import com.haji.racing.ui.components.RacingCard
import com.haji.racing.ui.components.RacingTextField
import com.haji.racing.ui.components.ScreenTitle
import com.haji.racing.ui.components.SectionTitle
import com.haji.racing.ui.components.StatTile
import com.haji.racing.ui.theme.RacingAccent
import com.haji.racing.ui.theme.RacingCardElevated
import com.haji.racing.ui.theme.RacingEnd
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.RacingPrimaryLight
import com.haji.racing.ui.theme.TextPrimary
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.theme.TextTertiary

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val trackCount by viewModel.trackCount.collectAsStateWithLifecycle()

    var showEditName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var showClearRecords by remember { mutableStateOf(false) }
    var showClearTracks by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        ScreenTitle("我的")
        Spacer(Modifier.height(8.dp))

        // ===== 头像 + 昵称 =====
        RacingCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(RacingPrimaryLight, RacingPrimary),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = nickname.first().uppercase(),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = nickname,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = {
                            nameInput = nickname
                            showEditName = true
                        }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "修改昵称",
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Text(
                        text = "本地用户 · 无需登录",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ===== 统计 =====
        SectionTitle("累计数据")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "总距离",
                value = FormatUtils.formatDistance(stats.totalDistance),
                icon = Icons.Filled.Route,
                modifier = Modifier.weight(1f),
                accent = RacingPrimary,
            )
            StatTile(
                label = "总时长",
                value = TimeUtils.formatDuration(stats.totalDurationMs),
                icon = Icons.Filled.AccessTime,
                modifier = Modifier.weight(1f),
                accent = RacingAccent,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "总记录",
                value = "${stats.totalRecordings}",
                icon = Icons.Filled.SportsMotorsports,
                modifier = Modifier.weight(1f),
                accent = RacingPrimary,
            )
            StatTile(
                label = "完成",
                value = "${stats.completedRecordings}",
                icon = Icons.Filled.CheckCircle,
                modifier = Modifier.weight(1f),
                accent = com.haji.racing.ui.theme.RacingStart,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "最快成绩",
                value = stats.bestTimeMs?.let { TimeUtils.formatDurationPrecise(it) } ?: "--:--",
                icon = Icons.Filled.Flag,
                modifier = Modifier.weight(1f),
                accent = com.haji.racing.ui.theme.MedalGold,
                valueColor = com.haji.racing.ui.theme.MedalGold,
            )
            StatTile(
                label = "赛道",
                value = "${trackCount}",
                icon = Icons.Filled.Flag,
                modifier = Modifier.weight(1f),
                accent = RacingAccent,
            )
        }
        Spacer(Modifier.height(24.dp))

        // ===== 数据管理 =====
        SectionTitle("数据管理")
        Spacer(Modifier.height(10.dp))
        RacingCard {
            DataRow(
                icon = Icons.Filled.DeleteForever,
                title = "清空全部记录",
                subtitle = "删除所有轨迹记录（共 ${stats.totalRecordings} 条）",
                tint = RacingEnd,
                onClick = { showClearRecords = true },
            )
            Spacer(Modifier.height(6.dp))
            DataRow(
                icon = Icons.Filled.Flag,
                title = "清空全部赛道",
                subtitle = "删除所有自定义赛道及其记录（共 $trackCount 条）",
                tint = RacingEnd,
                onClick = { showClearTracks = true },
            )
        }
        Spacer(Modifier.height(24.dp))

        // ===== 关于 =====
        SectionTitle("关于")
        Spacer(Modifier.height(10.dp))
        RacingCard {
            AboutRow("版本", "v2.0.0")
            AboutRow("定位坐标", "GCJ-02（高德地图）")
            AboutRow("数据存储", "仅本机 · 无账号 · 无云端")
            AboutRow("跟跑触发", "起点围栏 + 15 km/h 自动计时")
        }

        Spacer(Modifier.weight(1f))
    }

    // ===== 修改昵称 =====
    if (showEditName) {
        AlertDialog(
            onDismissRequest = { showEditName = false },
            containerColor = RacingCardElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("修改昵称", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                RacingTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = "昵称",
                    placeholder = "输入新昵称",
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNickname(nameInput)
                    showEditName = false
                }) {
                    Text("保存", color = RacingPrimaryLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditName = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
        )
    }

    if (showClearRecords) {
        ConfirmDialog(
            title = "清空全部记录？",
            message = "将删除所有 ${stats.totalRecordings} 条记录及其轨迹点，且不可恢复。",
            confirmText = "清空",
            destructive = true,
            onConfirm = {
                showClearRecords = false
                viewModel.clearAllRecords()
            },
            onDismiss = { showClearRecords = false },
        )
    }

    if (showClearTracks) {
        ConfirmDialog(
            title = "清空全部赛道？",
            message = "将删除所有 $trackCount 条赛道及其下的记录与轨迹，且不可恢复。",
            confirmText = "清空",
            destructive = true,
            onConfirm = {
                showClearTracks = false
                viewModel.clearAllTracks()
            },
            onDismiss = { showClearTracks = false },
        )
    }
}

@Composable
private fun DataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(110.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}
