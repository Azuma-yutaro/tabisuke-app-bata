package com.example.tabisuke.ui.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.tabisuke.utils.PdfGenerator
import com.example.tabisuke.ui.scheduledetail.ScheduleDetailModal
import com.example.tabisuke.ui.scheduledetail.Schedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, groupId: String, eventId: String) {
    val viewModel: MainViewModel = viewModel()
    val event by viewModel.event.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    var showScheduleDetail by remember { mutableStateOf<Schedule?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted: Boolean ->
        if (isGranted) {
            event?.let { eventData ->
                val file = PdfGenerator.generatePdf(context, eventData, schedules, "${eventData.title}.pdf")
                if (file != null) {
                    Toast.makeText(context, "PDFをダウンロードしました: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "PDFのダウンロードに失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "PDFダウンロードにはストレージ権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(groupId, eventId) {
        viewModel.fetchEvent(groupId, eventId)
        viewModel.loadSchedules(groupId, eventId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = event?.title ?: "イベント",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) }) {
                        Icon(Icons.Filled.Share, contentDescription = "PDFをダウンロード")
                    }
                    IconButton(onClick = { navController.navigate("management/${groupId}/${eventId}") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "管理画面")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                listOf(
                    Triple("行事登録", "schedule_edit", Icons.Filled.Add),
                    Triple("メイン", "main_home", Icons.Filled.Home),
                    Triple("マップ", "map_list", Icons.Filled.LocationOn)
                ).forEach { (name, route, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(name) },
                        selected = currentDestination?.route == route,
                        onClick = {
                            when (route) {
                                "schedule_edit" -> navController.navigate("schedule_edit/${groupId}/${eventId}")
                                "map_list" -> {
                                    event?.mapUrl?.let { url ->
                                        if (url.isNotBlank()) {
                                            uriHandler.openUri(url)
                                        } else {
                                            Toast.makeText(context, "マップURLが設定されていません", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // メインコンテンツ
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // 行事リスト（日付ごとにグループ化）
                val groupedSchedules = schedules.groupBy { it.date }.toSortedMap()
                
                groupedSchedules.forEach { (date, dailySchedules) ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = date,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    items(dailySchedules.sortedBy { it.time }) { schedule ->
                        ScheduleItem(
                            schedule = schedule,
                            onClick = { showScheduleDetail = schedule }
                        )
                    }
                }

                // オリジナルボタン
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        event?.button1?.let { buttonConfig ->
                            if (buttonConfig.text.isNotBlank()) {
                                CustomButton(
                                    text = buttonConfig.text,
                                    icon = getIconFromString(buttonConfig.icon),
                                    onClick = { 
                                        if (buttonConfig.url.isNotBlank()) uriHandler.openUri(buttonConfig.url) 
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        event?.button2?.let { buttonConfig ->
                            if (buttonConfig.text.isNotBlank()) {
                                CustomButton(
                                    text = buttonConfig.text,
                                    icon = getIconFromString(buttonConfig.icon),
                                    onClick = { 
                                        if (buttonConfig.url.isNotBlank()) uriHandler.openUri(buttonConfig.url) 
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        event?.button3?.let { buttonConfig ->
                            if (buttonConfig.text.isNotBlank()) {
                                CustomButton(
                                    text = buttonConfig.text,
                                    icon = getIconFromString(buttonConfig.icon),
                                    onClick = { 
                                        if (buttonConfig.url.isNotBlank()) uriHandler.openUri(buttonConfig.url) 
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // デフォルトボタン
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { navController.navigate("budget/${groupId}/${eventId}") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("予算管理", fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = { requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ダウンロード", fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = { navController.navigate("management/${groupId}/${eventId}") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("管理", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Footer - イベント一覧ボタン
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Button(
                    onClick = { navController.navigate("event_list/${groupId}") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("イベント一覧に戻る", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
    
    // 行事詳細モーダル
    showScheduleDetail?.let { schedule ->
        ScheduleDetailModal(
            schedule = schedule,
            onDismiss = { showScheduleDetail = null },
            onUrlClick = { url -> uriHandler.openUri(url) }
        )
    }
}

@Composable
fun ScheduleItem(
    schedule: Schedule,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = schedule.time,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = schedule.title,
                modifier = Modifier.weight(2f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${schedule.budget}円",
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun CustomButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        contentPadding = PaddingValues(12.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

fun getIconFromString(iconName: String): ImageVector? {
    return when (iconName) {
        "Add" -> Icons.Filled.Add
        "Settings" -> Icons.Filled.Settings
        "Share" -> Icons.Filled.Share
        "Home" -> Icons.Filled.Home
        "Info" -> Icons.Filled.Info
        "LocationOn" -> Icons.Filled.LocationOn
        "DateRange" -> Icons.Filled.DateRange
        "List" -> Icons.Filled.List
        else -> Icons.Filled.Info // デフォルト
    }
}