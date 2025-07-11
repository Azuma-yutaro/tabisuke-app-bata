package com.example.tabisuke.ui.grouplist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    navController: NavController,
    viewModel: GroupListViewModel = viewModel()
) {
    val groups by viewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val pendingOperationsCount by viewModel.pendingOperationsCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadGroups()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "グループ一覧",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (isOffline) {
                            Text(
                                text = "オフライン",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("mypage") }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "マイページ",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // ログアウトボタン（削除）
                }
            )
        },
        floatingActionButton = {
            Column {
                // グループに参加ボタン
                FloatingActionButton(
                    onClick = { navController.navigate("join_group") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("参加", fontSize = 12.sp)
                }
                
                // グループを作成ボタン
                FloatingActionButton(
                    onClick = { navController.navigate("create_group") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, "グループを作成")
                }
            }
        }
    ) { padding ->
        Column {
            // オフライン時の通知バナー
            if (isOffline) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "オフライン",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "オフラインです。一部の機能が制限されます。",
                            fontSize = 14.sp
                        )
                        if (pendingOperationsCount > 0) {
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSyncing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "同期中...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                Text(
                                    text = "${pendingOperationsCount}件の保留中",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            
            // メインコンテンツ
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(groups) { group ->
                        // パステルカラー配列
                        val pastelColors = listOf(
                            Color(0xFFFFF4E6), // オレンジ系
                            Color(0xFFE6F7FF), // 水色系
                            Color(0xFFE6FFF4), // 緑系
                            Color(0xFFF4E6FF), // 紫系
                            Color(0xFFFFE6F7), // ピンク系
                            Color(0xFFFFFBE6)  // 黄色系
                        )
                        // グループごとに色を決定（IDのハッシュで）
                        val bgColor = pastelColors[(group.id.hashCode().absoluteValue) % pastelColors.size]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("event_list/${group.id}") }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 画像 or パステル色＋頭文字
                            if (group.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = group.imageUrl,
                                    contentDescription = "グループ画像",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = group.name.firstOrNull()?.toString() ?: "?",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = group.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "メンバー数: ${group.memberCount}人",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "イベント数: ${group.eventCount}件",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { navController.navigate("group_settings/${group.id}") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "グループ設定",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(start = 80.dp))
                    }
                }
            }
        }
    }
} 