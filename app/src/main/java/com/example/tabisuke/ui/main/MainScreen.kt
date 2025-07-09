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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val schedulesWithDates by viewModel.schedulesWithDates.collectAsState()
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
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "tabisuke",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = androidx.compose.ui.graphics.Color(0xFF833AB4)
                            )
                            Text(
                                text = "|",
                                fontSize = 14.sp,
                                color = androidx.compose.ui.graphics.Color(0xFFCCCCCC)
                            )
                            Text(
                                text = event?.title ?: "イベント",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = androidx.compose.ui.graphics.Color(0xFF333333)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                ),
                actions = {
                    IconButton(onClick = { requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) }) {
                        Icon(Icons.Filled.Share, contentDescription = "PDFをダウンロード", tint = androidx.compose.ui.graphics.Color(0xFF666666))
                    }
                    IconButton(onClick = { navController.navigate("management/${groupId}/${eventId}") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "管理画面", tint = androidx.compose.ui.graphics.Color(0xFF666666))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(vertical = 0.dp)
                    .height(56.dp)
                    .background(androidx.compose.ui.graphics.Color.White)
                    .border(
                        width = 0.5.dp,
                        color = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ),
                containerColor = androidx.compose.ui.graphics.Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                listOf(
                    Pair("schedule_edit", Icons.Filled.Add),
                    Pair("main_home", Icons.Filled.Home),
                    Pair("map_list", Icons.Filled.LocationOn)
                ).forEach { (route, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = { },
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
                // 行事リスト（日数ごとにグループ化）
                val groupedSchedules = schedulesWithDates.groupBy { it.schedule.dayNumber }.toSortedMap()
                
                groupedSchedules.forEach { (dayNumber, dailySchedules) ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .border(
                                    width = 1.dp,
                                    color = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = androidx.compose.ui.graphics.Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${dayNumber}日目",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = androidx.compose.ui.graphics.Color(0xFF333333)
                                    )
                                    // 実際の日付を表示
                                    dailySchedules.firstOrNull()?.actualDate?.let { actualDate ->
                                        Text(
                                            text = actualDate,
                                            fontSize = 14.sp,
                                            color = androidx.compose.ui.graphics.Color(0xFF666666)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    items(dailySchedules.sortedBy { it.schedule.time }) { scheduleWithDate ->
                        ScheduleItem(
                            schedule = scheduleWithDate.schedule,
                            onClick = { showScheduleDetail = scheduleWithDate.schedule },
                            backgroundColor = when (dayNumber) {
                                1 -> androidx.compose.ui.graphics.Color(0xFFFFF3E0) // オレンジ系
                                2 -> androidx.compose.ui.graphics.Color(0xFFE8F5E8) // グリーン系
                                3 -> androidx.compose.ui.graphics.Color(0xFFE3F2FD) // ブルー系
                                4 -> androidx.compose.ui.graphics.Color(0xFFFCE4EC) // ピンク系
                                5 -> androidx.compose.ui.graphics.Color(0xFFF3E5F5) // パープル系
                                6 -> androidx.compose.ui.graphics.Color(0xFFE0F2F1) // ティール系
                                else -> androidx.compose.ui.graphics.Color(0xFFFAFAFA)
                            }
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
            onUrlClick = { url -> uriHandler.openUri(url) },
            onEdit = { scheduleToEdit ->
                // 編集画面に遷移
                navController.navigate("schedule_edit/${groupId}/${eventId}/${scheduleToEdit.id}")
                showScheduleDetail = null
            }
        )
    }
}

@Composable
fun ScheduleItem(
    schedule: Schedule,
    onClick: () -> Unit,
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .border(
                width = 1.dp,
                color = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
            if (schedule.budget > 0) {
                Text(
                    text = "${schedule.budget}円",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
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