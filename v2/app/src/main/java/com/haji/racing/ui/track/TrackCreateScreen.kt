package com.haji.racing.ui.track

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.haji.racing.domain.model.FencePoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackCreateScreen(
    navController: NavController,
    viewModel: TrackCreateViewModel = hiltViewModel(),
) {
    var trackName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var creatorName by remember { mutableStateOf("") }

    // 起点围栏坐标
    var startLat by remember { mutableStateOf("") }
    var startLng by remember { mutableStateOf("") }

    // 终点围栏坐标
    var endLat by remember { mutableStateOf("") }
    var endLng by remember { mutableStateOf("") }

    val isSaved by viewModel.isSaved.collectAsState()

    LaunchedEffect(isSaved) {
        if (isSaved) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建赛道") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val startFence = listOf(
                                FencePoint(startLat.toDoubleOrNull() ?: 0.0, startLng.toDoubleOrNull() ?: 0.0)
                            )
                            val endFence = listOf(
                                FencePoint(endLat.toDoubleOrNull() ?: 0.0, endLng.toDoubleOrNull() ?: 0.0)
                            )
                            viewModel.saveTrack(
                                name = trackName,
                                description = description,
                                creatorName = creatorName,
                                startFencePoints = startFence,
                                endFencePoints = endFence,
                            )
                        },
                        enabled = trackName.isNotBlank() && startLat.isNotBlank() && startLng.isNotBlank() && endLat.isNotBlank() && endLng.isNotBlank(),
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // 赛道基本信息
            Text("赛道信息", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = trackName,
                onValueChange = { trackName = it },
                label = { Text("赛道名称 *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = creatorName,
                onValueChange = { creatorName = it },
                label = { Text("创建人") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 起点围栏
            Text("起点围栏", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = startLat,
                    onValueChange = { startLat = it },
                    label = { Text("纬度 *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = startLng,
                    onValueChange = { startLng = it },
                    label = { Text("经度 *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 终点围栏
            Text("终点围栏", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = endLat,
                    onValueChange = { endLat = it },
                    label = { Text("纬度 *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = endLng,
                    onValueChange = { endLng = it },
                    label = { Text("经度 *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("使用说明", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 输入赛道名称和创建人信息\n" +
                                "2. 输入起点和终点的经纬度坐标\n" +
                                "3. 保存后赛道将显示在赛道列表中\n" +
                                "4. 录制时选择赛道即可使用跟跑模式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
