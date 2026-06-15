package com.haji.racing.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.haji.racing.core.common.FormatUtils
import com.haji.racing.data.repository.SyncResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.currentUser.collectAsState()
    val totalDistance by viewModel.totalDistance.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("用户中心") })
        }
    ) { padding ->
        if (!isLoggedIn) {
            LoginView(
                modifier = Modifier.padding(padding),
                onLogin = { navController.navigate("login") },
            )
        } else {
            ProfileView(
                modifier = Modifier.padding(padding),
                user = user,
                totalDistance = totalDistance,
                isSyncing = isSyncing,
                lastSyncResult = lastSyncResult,
                onEdit = { showEditDialog = true },
                onLogout = { viewModel.logout() },
                onSync = { viewModel.sync() },
            )
        }
    }

    if (showEditDialog && user != null) {
        EditProfileDialog(
            currentNickname = user!!.nickname,
            onSave = { nickname ->
                viewModel.updateNickname(nickname)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }
}

@Composable
private fun LoginView(modifier: Modifier = Modifier, onLogin: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("欢迎使用 Haji Racing", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("登录后可以同步轨迹数据", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("登录")
        }
    }
}

@Composable
private fun ProfileView(
    modifier: Modifier = Modifier,
    user: com.haji.racing.domain.model.User?,
    totalDistance: Double,
    isSyncing: Boolean,
    lastSyncResult: SyncResult?,
    onEdit: () -> Unit,
    onLogout: () -> Unit,
    onSync: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(96.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "头像",
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = user?.nickname ?: "", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "UID: ${user?.uid ?: ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("驾驶统计", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem("累积里程", FormatUtils.formatDistance(totalDistance))
                    StatItem("轨迹数量", "${user?.totalRecordings ?: 0}")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("修改资料")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSync,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSyncing,
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("同步中...")
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("同步数据")
            }
        }

        lastSyncResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "上传: ${result.tracksUploaded}赛道 ${result.recordingsUploaded}轨迹 | 下载: ${result.tracksDownloaded}赛道 ${result.recordingsDownloaded}轨迹",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onLogout) {
            Text("退出登录", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EditProfileDialog(
    currentNickname: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nickname by remember { mutableStateOf(currentNickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改资料") },
        text = {
            Column {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(nickname) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
