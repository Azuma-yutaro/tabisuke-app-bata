package com.example.tabisuke.ui.management

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.time.format.DateTimeFormatter
import com.example.tabisuke.ui.main.EventBottomNavBar
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconDropdown(
    label: String,
    selectedValue: String?,
    onSelect: (String) -> Unit
) {
    val iconOptions = listOf(
        "情報" to "info",
        "車" to "car",
        "電車" to "train",
        "飛行機" to "air",
        "宿" to "hotel",
        "食べ物" to "food"
    )
    var expanded by remember { mutableStateOf(false) }
    val selectedIcon = iconOptions.find { it.second == (selectedValue ?: "") } ?: iconOptions[0]
    val context = LocalContext.current
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedIcon.first,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            iconOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.first) },
                    onClick = {
                        onSelect(option.second)
                        expanded = false
                    },
                    leadingIcon = {
                        val resId = context.resources.getIdentifier(option.second, "drawable", context.packageName)
                        if (resId != 0) {
                            Icon(
                                painter = painterResource(id = resId),
                                contentDescription = option.first,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementScreen(
    navController: NavController,
    groupId: String,
    eventId: String,
    viewModel: ManagementViewModel = viewModel()
) {
    val event by viewModel.event.collectAsState()
    val showDeleteConfirm by viewModel.showDeleteConfirm.collectAsState()
    val showFinalDeleteConfirm by viewModel.showFinalDeleteConfirm.collectAsState()
    val context = LocalContext.current
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val isEventCreator by viewModel.isEventCreator.collectAsState()

    // 日付選択状態
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    LaunchedEffect(Unit) {
        viewModel.loadEvent(groupId, eventId)
    }
    
    // 開始日のデフォルト値を今日に設定
    LaunchedEffect(event) {
        if (event?.startDate.isNullOrEmpty()) {
            val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            viewModel.updateEventStartDate(today)
        }
    }

    // --- ここでエラー変数を宣言 ---
    var mapUrlError by remember { mutableStateOf("") }
    var button1UrlError by remember { mutableStateOf("") }
    var button2UrlError by remember { mutableStateOf("") }
    var button3UrlError by remember { mutableStateOf("") }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "管理画面",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    if (isEventCreator) {
                        IconButton(
                            onClick = {
                                viewModel.showDeleteConfirm()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "削除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            EventBottomNavBar(
                navController = navController,
                groupId = groupId,
                eventId = eventId,
                mapUrl = null
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var mapUrlError by remember { mutableStateOf("") }
            var button1UrlError by remember { mutableStateOf("") }
            var button2UrlError by remember { mutableStateOf("") }
            var button3UrlError by remember { mutableStateOf("") }
            
            // 削除確認ダイアログ（1回目）
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { viewModel.hideDeleteConfirm() },
                    title = {
                        Text(
                            text = "イベント削除の確認",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    text = {
                        Text(
                            text = "このイベントを削除しますか？\n\n削除すると、このイベントに関連するすべてのスケジュールも削除されます。",
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.showFinalDeleteConfirm() }
                        ) {
                            Text(
                                text = "削除する",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.hideDeleteConfirm() }
                        ) {
                            Text("キャンセル")
                        }
                    }
                )
            }
            
            // 最終削除確認ダイアログ（2回目）
            if (showFinalDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { viewModel.hideFinalDeleteConfirm() },
                    title = {
                        Text(
                            text = "最終確認",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    text = {
                        Text(
                            text = "この操作は取り消せません。\n\n本当に削除しますか？",
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.executeDeleteEvent(
                                    groupId = groupId,
                                    eventId = eventId,
                                    onSuccess = {
                                        Toast.makeText(context, "イベントを削除しました", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(context, "削除に失敗しました: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        ) {
                            Text(
                                text = "削除する",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.hideFinalDeleteConfirm() }
                        ) {
                            Text("キャンセル")
                        }
                    }
                )
            }
            // イベントタイトル
            OutlinedTextField(
                value = event?.title ?: "",
                onValueChange = { 
                    if (it.length <= 20) {
                        viewModel.updateEventTitle(it) 
                    }
                },
                label = { Text("イベントタイトル *") },
                placeholder = { Text("最大20文字") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = (event?.title ?: "").isEmpty() && (event?.title ?: "").isNotEmpty(),
                supportingText = {
                    Text("${(event?.title ?: "").length}/20")
                }
            )

            // 説明
            OutlinedTextField(
                value = event?.description ?: "",
                onValueChange = { 
                    if (it.length <= 100) {
                        viewModel.updateEventDescription(it) 
                    }
                },
                label = { Text("説明") },
                placeholder = { Text("最大100文字") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                supportingText = {
                    Text("${(event?.description ?: "").length}/100")
                }
            )

            // 期間（日付入力改善）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = event?.startDate ?: "",
                    onValueChange = { },
                    label = { Text("開始日 *") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("カレンダーから選択") },
                    isError = (event?.startDate ?: "").isEmpty() && (event?.startDate ?: "").isNotEmpty(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { viewModel.showStartDatePicker() }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "カレンダーを開く"
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = event?.endDate ?: "",
                    onValueChange = { },
                    label = { Text("終了日 *") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("カレンダーから選択") },
                    isError = (event?.endDate ?: "").isEmpty() && (event?.endDate ?: "").isNotEmpty(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { viewModel.showEndDatePicker() }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "カレンダーを開く"
                            )
                        }
                    }
                )
            }

            // 日付バリデーションメッセージ
            var dateValidationMessage by remember { mutableStateOf("") }
            var isDateValid by remember { mutableStateOf(true) }
            
            LaunchedEffect(event?.startDate, event?.endDate) {
                if (!event?.startDate.isNullOrEmpty() && !event?.endDate.isNullOrEmpty()) {
                    try {
                        val start = java.time.LocalDate.parse(event?.startDate, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val end = java.time.LocalDate.parse(event?.endDate, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        
                        if (end.isBefore(start)) {
                            isDateValid = false
                            dateValidationMessage = "終了日は開始日以降にしてください"
                        } else {
                            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end)
                            if (daysBetween > 30) {
                                isDateValid = false
                                dateValidationMessage = "期間は最大30日までです"
                            } else {
                                isDateValid = true
                                dateValidationMessage = ""
                            }
                        }
                    } catch (e: Exception) {
                        // 日付パースエラーの場合はバリデーションをスキップ
                    }
                }
            }
            
            if (dateValidationMessage.isNotEmpty()) {
                Text(
                    text = dateValidationMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )
            }

                        // Google Maps URL
            val mapUrl = event?.mapUrl ?: ""
                OutlinedTextField(
                    value = mapUrl,
                onValueChange = {
                    if (it.length <= 100) {
                        viewModel.updateEventMapUrl(it)
                        mapUrlError = if (it.isNotEmpty() && !(it.startsWith("http://") || it.startsWith("https://"))) {
                            "正しいURLを入力してください"
                        } else {
                            ""
                        }
                    }
                },
                label = { Text("Google Maps URL") },
                placeholder = { Text("https://maps.google.com/... (最大100文字)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = mapUrlError.isNotEmpty(),
                supportingText = {
                    Text("${mapUrl.length}/100")
                }
            )
            if (mapUrlError.isNotEmpty()) {
                Text(
                    text = mapUrlError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )
            }

            // 権限設定
            if (isEventCreator) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "グループメンバー権限設定",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "この操作はグループ全体に適応されます",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 権限の説明
                        Text(
                            text = "• オーナー: 完全な管理権限\n• 編集者: グループにメンバーの追加・削除を行えます\n• 閲覧者: 閲覧のみ\n• ゲスト: シリアルコードでアクセス",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                Spacer(modifier = Modifier.height(16.dp))
                        
                        // メンバー権限管理
                        val members by viewModel.members.collectAsState()
                        
                        Text(
                            text = "メンバー権限管理",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (members.isEmpty()) {
                            Text(
                                text = "メンバーが登録されていません",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            members.forEach { member ->
                                // イベント作成者以外のメンバーのみドロップダウンを表示
                                if (member.personalId != viewModel.getCurrentUserId()) {
                                    MemberPermissionItem(
                                        member = member,
                                        onRoleChange = { newRole ->
                                            viewModel.updateMemberRole(
                                                groupId = groupId,
                                                personalId = member.personalId,
                                                newRole = newRole,
                                                onSuccess = {
                                                    Toast.makeText(context, "${member.name}の権限を${newRole}に変更しました", Toast.LENGTH_SHORT).show()
                                                },
                                                onFailure = { e ->
                                                    Toast.makeText(context, "権限変更に失敗しました: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    )
                                } else {
                                    // イベント作成者は読み取り専用で表示
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = member.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            
                                            OutlinedTextField(
                                                value = "オーナー",
                                                onValueChange = {},
                                                readOnly = true,
                                                modifier = Modifier.width(100.dp),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // ゲストアクセス設定
                        val guestAccess by viewModel.guestAccess.collectAsState()
                        
                        Text(
                            text = "ゲストアクセス設定",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                            Text(
                                text = "シリアルコードでアクセスを許可",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                    Switch(
                                checked = guestAccess.enabled,
                                onCheckedChange = { viewModel.updateGuestAccessEnabled(it) }
                    )
                }
                        
                if (guestAccess.enabled) {
                    val guestUrl = "https://tabisuke-web.vercel.app/guest_access/${groupId}/${eventId}"
                    val context = LocalContext.current
                    val uriHandler = LocalUriHandler.current
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        // 案内文
                        Text(
                            text = "このURLを共有することで、Webで誰でもこのイベントを確認することができます。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "表示のみ可能（編集不可）",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // シリアルコード入力欄
                    OutlinedTextField(
                            value = guestAccess.serialCode,
                            onValueChange = { viewModel.updateGuestAccessSerialCode(it) },
                        label = { Text("シリアルコード") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("例: ABC123") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // URLを開く・シェアボタン（横並び）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                        ) {
                            Button(
                                onClick = { uriHandler.openUri(guestUrl) },
                                modifier = Modifier
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = "URLを開く")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("URLを開く")
                            }
                            Button(
                                onClick = {
                                    val shareText = "イベントのしおりが作成されました！\n下記URLから確認しましょう！\n$guestUrl\nシリアルコード\n${guestAccess.serialCode}"
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "シェア")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("シェア")
                            }
                        }
                    }
                }
                    }
                }
            }

            // オリジナルボタン設定
            TabbedButtonSettings(
                event = event,
                viewModel = viewModel,
                button1UrlError = button1UrlError,
                button2UrlError = button2UrlError,
                button3UrlError = button3UrlError,
                onButton1UrlErrorChange = { button1UrlError = it },
                onButton2UrlErrorChange = { button2UrlError = it },
                onButton3UrlErrorChange = { button3UrlError = it }
            )

            // 保存ボタン
                Button(
                onClick = {
                    viewModel.saveEvent(
                        groupId = groupId,
                        eventId = eventId,
                        onSuccess = {
                            Toast.makeText(context, "保存しました", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "保存に失敗しました: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !(event?.title.isNullOrEmpty()) && 
                         !(event?.startDate.isNullOrEmpty()) && 
                         !(event?.endDate.isNullOrEmpty()) && 
                         isDateValid &&
                         mapUrlError.isEmpty() &&
                         button1UrlError.isEmpty() &&
                         button2UrlError.isEmpty() &&
                         button3UrlError.isEmpty()
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    
    // 日付選択ダイアログ
    val showStartDatePicker by viewModel.showStartDatePicker.collectAsState()
    val showEndDatePicker by viewModel.showEndDatePicker.collectAsState()
    
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.hideStartDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            viewModel.updateEventStartDate(date.format(formatter))
                        }
                        viewModel.hideStartDatePicker()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideStartDatePicker() }) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(
                state = startDatePickerState,
                title = { Text("開始日を選択") }
            )
        }
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.hideEndDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            viewModel.updateEventEndDate(date.format(formatter))
                        }
                        viewModel.hideEndDatePicker()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideEndDatePicker() }) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(
                state = endDatePickerState,
                title = { Text("終了日を選択") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPermissionItem(
    member: Member,
    onRoleChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("owner", "editor", "viewer")

    // 権限の日本語表示
    fun getRoleDisplayName(role: String): String {
        return when (role) {
            "owner" -> "オーナー"
            "editor" -> "編集者"
            "viewer" -> "閲覧者"
            else -> role
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
            Text(
                text = member.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
        ExposedDropdownMenuBox(
            expanded = expanded,
                onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                    value = getRoleDisplayName(member.role),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .width(100.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                roles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(getRoleDisplayName(role)) },
                            onClick = {
                        onRoleChange(role)
                        expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedButtonSettings(
    event: Event?,
    viewModel: ManagementViewModel,
    button1UrlError: String,
    button2UrlError: String,
    button3UrlError: String,
    onButton1UrlErrorChange: (String) -> Unit,
    onButton2UrlErrorChange: (String) -> Unit,
    onButton3UrlErrorChange: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("ボタン1", "ボタン2", "ボタン3")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "オリジナルボタン設定",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // タブ行
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // タブコンテンツ
            when (selectedTabIndex) {
                0 -> Button1Content(
                    event = event,
                    viewModel = viewModel,
                    button1UrlError = button1UrlError,
                    onButton1UrlErrorChange = onButton1UrlErrorChange
                )
                1 -> Button2Content(
                    event = event,
                    viewModel = viewModel,
                    button2UrlError = button2UrlError,
                    onButton2UrlErrorChange = onButton2UrlErrorChange
                )
                2 -> Button3Content(
                    event = event,
                    viewModel = viewModel,
                    button3UrlError = button3UrlError,
                    onButton3UrlErrorChange = onButton3UrlErrorChange
                )
            }
        }
    }
}

@Composable
fun Button1Content(
    event: Event?,
    viewModel: ManagementViewModel,
    button1UrlError: String,
    onButton1UrlErrorChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = event?.button1?.text ?: "",
            onValueChange = { 
                if (it.length <= 100) {
                    viewModel.updateButton1Text(it) 
                }
            },
            label = { Text("ボタン1 テキスト") },
            placeholder = { Text("最大100文字") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text("${(event?.button1?.text ?: "").length}/100")
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        val button1Url = event?.button1?.url ?: ""
        OutlinedTextField(
            value = button1Url,
            onValueChange = {
                if (it.length <= 100) {
                    viewModel.updateButton1Url(it)
                    onButton1UrlErrorChange(
                        if (it.isNotEmpty() && !(it.startsWith("http://") || it.startsWith("https://"))) {
                            "正しいURLを入力してください"
                        } else {
                            ""
                        }
                    )
                }
            },
            label = { Text("ボタン1 URL") },
            placeholder = { Text("https://... (最大100文字)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = button1UrlError.isNotEmpty(),
            supportingText = {
                Text("${button1Url.length}/100")
            }
        )
        if (button1UrlError.isNotEmpty()) {
            Text(
                text = button1UrlError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        IconDropdown(
            label = "ボタン1 アイコン",
            selectedValue = event?.button1?.icon,
            onSelect = { viewModel.updateButton1Icon(it) }
        )
    }
}

@Composable
fun Button2Content(
    event: Event?,
    viewModel: ManagementViewModel,
    button2UrlError: String,
    onButton2UrlErrorChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = event?.button2?.text ?: "",
            onValueChange = { 
                if (it.length <= 100) {
                    viewModel.updateButton2Text(it) 
                }
            },
            label = { Text("ボタン2 テキスト") },
            placeholder = { Text("最大100文字") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text("${(event?.button2?.text ?: "").length}/100")
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        val button2Url = event?.button2?.url ?: ""
        OutlinedTextField(
            value = button2Url,
            onValueChange = {
                if (it.length <= 100) {
                    viewModel.updateButton2Url(it)
                    onButton2UrlErrorChange(
                        if (it.isNotEmpty() && !(it.startsWith("http://") || it.startsWith("https://"))) {
                            "正しいURLを入力してください"
                        } else {
                            ""
                        }
                    )
                }
            },
            label = { Text("ボタン2 URL") },
            placeholder = { Text("https://... (最大100文字)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = button2UrlError.isNotEmpty(),
            supportingText = {
                Text("${button2Url.length}/100")
            }
        )
        if (button2UrlError.isNotEmpty()) {
            Text(
                text = button2UrlError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        IconDropdown(
            label = "ボタン2 アイコン",
            selectedValue = event?.button2?.icon,
            onSelect = { viewModel.updateButton2Icon(it) }
        )
    }
}

@Composable
fun Button3Content(
    event: Event?,
    viewModel: ManagementViewModel,
    button3UrlError: String,
    onButton3UrlErrorChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = event?.button3?.text ?: "",
            onValueChange = { 
                if (it.length <= 100) {
                    viewModel.updateButton3Text(it) 
                }
            },
            label = { Text("ボタン3 テキスト") },
            placeholder = { Text("最大100文字") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text("${(event?.button3?.text ?: "").length}/100")
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        val button3Url = event?.button3?.url ?: ""
        OutlinedTextField(
            value = button3Url,
            onValueChange = {
                if (it.length <= 100) {
                    viewModel.updateButton3Url(it)
                    onButton3UrlErrorChange(
                        if (it.isNotEmpty() && !(it.startsWith("http://") || it.startsWith("https://"))) {
                            "正しいURLを入力してください"
                        } else {
                            ""
                        }
                    )
                }
            },
            label = { Text("ボタン3 URL") },
            placeholder = { Text("https://... (最大100文字)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = button3UrlError.isNotEmpty(),
            supportingText = {
                Text("${button3Url.length}/100")
            }
        )
        if (button3UrlError.isNotEmpty()) {
            Text(
                text = button3UrlError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        IconDropdown(
            label = "ボタン3 アイコン",
            selectedValue = event?.button3?.icon,
            onSelect = { viewModel.updateButton3Icon(it) }
        )
    }
}