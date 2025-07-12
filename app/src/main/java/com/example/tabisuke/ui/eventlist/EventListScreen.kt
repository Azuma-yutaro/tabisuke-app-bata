package com.example.tabisuke.ui.eventlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.absoluteValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.example.tabisuke.R
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    navController: NavController,
    groupId: String,
    viewModel: EventListViewModel = viewModel()
) {
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val pendingOperationsCount by viewModel.pendingOperationsCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(groupId) {
        viewModel.loadEvents(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "イベント一覧",
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
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("group_list") }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("mypage") }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "マイページ",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_event/$groupId") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, "イベントを作成")
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
            } else if (hasError) {
                // エラー状態の表示
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "エラー",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "データの読み込みに失敗しました",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
            Spacer(modifier = Modifier.height(8.dp))
                    
                    errorMessage?.let { message ->
                        Text(
                            text = message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
            Button(
                        onClick = { 
                            viewModel.clearError()
                            viewModel.loadEvents(groupId)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "再試行",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("再試行", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // イベントが一つもない場合の表示
                    if (events.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                
                                Text(
                                    text = "旅は、始まったばかりです",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Image(
                                    painter = painterResource(id = R.drawable.cat),
                                    contentDescription = "猫の画像",
                                    modifier = Modifier.size(120.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = "右下のボタンから、イベントを作成しよう",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    } else {
                        // 開始日順で並び替え（終了済みは除外）
                        val sortedEvents = events.sortedBy { event ->
                            try {
                                if (event.startDate.isNotEmpty()) {
                                    LocalDate.parse(event.startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                } else {
                                    LocalDate.MAX // 空の場合は最後に配置
                                }
                            } catch (e: DateTimeParseException) {
                                LocalDate.MAX // パースエラーの場合も最後に配置
                            }
                        }
                        
                        // 終了済みイベントを分離
                        val activeEvents = sortedEvents.filter { event ->
                            try {
                                if (event.endDate.isNotEmpty()) {
                                    val endDate = LocalDate.parse(event.endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                    val today = LocalDate.now()
                                    !endDate.isBefore(today)
                                } else {
                                    true // 終了日が空の場合はアクティブとして扱う
                                }
                            } catch (e: DateTimeParseException) {
                                true // パースエラーの場合はアクティブとして扱う
                            }
                        }
                        
                        val expiredEvents = sortedEvents.filter { event ->
                            try {
                                if (event.endDate.isNotEmpty()) {
                                    val endDate = LocalDate.parse(event.endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                    val today = LocalDate.now()
                                    endDate.isBefore(today)
                                } else {
                                    false
                                }
                            } catch (e: DateTimeParseException) {
                                false
                            }
                        }
                        
                        // アクティブイベントを表示
                        items(activeEvents) { event ->
                            // 淡い背景色の配列
                            val pastelColors = listOf(
                                Color(0xFFF3E4F5), // 淡い紫
                                Color(0xFFE1FEE8), // 淡い緑
                                Color(0xFFE1EFFD), // 淡い青
                                Color(0xFFFFF4E6), // 淡いオレンジ
                                Color(0xFFF4E6FF)  // 淡い紫
                            )
                            
                            // イベントごとに色を決定（IDのハッシュで）
                            val bgColor = pastelColors[(event.id.hashCode().absoluteValue) % pastelColors.size]
                            
                            // カード内テキスト色をダークモード時はColor.Blackで明度差を大きくするよう、Textのcolor指定を修正
                            val eventTitleColor = if (isDark) Color.Black else MaterialTheme.colorScheme.onSurface
                            val eventPeriodColor = if (isDark) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = bgColor
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                onClick = {
                                    navController.navigate("home/${groupId}/${event.id}")
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = event.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = eventTitleColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (event.startDate.isNotEmpty() && event.endDate.isNotEmpty()) {
                                        Text(
                                            text = "期間: ${event.startDate} - ${event.endDate}",
                                            fontSize = 14.sp,
                                            color = eventPeriodColor
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 終了済みイベントの区切り線
                        if (expiredEvents.isNotEmpty()) {
                            item {
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                Text(
                                    text = "終了済みイベント",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        // 終了済みイベントを表示
                        items(expiredEvents) { event ->
                            // 淡い背景色の配列
                            val pastelColors = listOf(
                                Color(0xFFF3E4F5), // 淡い紫
                                Color(0xFFE1FEE8), // 淡い緑
                                Color(0xFFE1EFFD), // 淡い青
                                Color(0xFFFFF4E6), // 淡いオレンジ
                                Color(0xFFF4E6FF)  // 淡い紫
                            )
                            
                            // イベントごとに色を決定（IDのハッシュで）
                            val bgColor = pastelColors[(event.id.hashCode().absoluteValue) % pastelColors.size]
                            
                            // カード内テキスト色をダークモード時はColor.Blackで明度差を大きくするよう、Textのcolor指定を修正
                            val expiredTitleColor = if (isDark) Color.Black.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            val expiredPeriodColor = if (isDark) Color.Black.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = bgColor.copy(alpha = 0.5f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                onClick = {
                                    navController.navigate("home/${groupId}/${event.id}")
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = event.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = expiredTitleColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (event.startDate.isNotEmpty() && event.endDate.isNotEmpty()) {
                                        Text(
                                            text = "期間: ${event.startDate} - ${event.endDate}",
                                            fontSize = 14.sp,
                                            color = expiredPeriodColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "終了済み",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}