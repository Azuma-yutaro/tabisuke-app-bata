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
            // 行事タイトル
            OutlinedTextField(
                value = event?.title ?: "",
                onValueChange = { viewModel.updateEventTitle(it) },
                label = { Text("行事タイトル") },
                modifier = Modifier.fillMaxWidth()
            )

            // 説明
            OutlinedTextField(
                value = event?.description ?: "",
                onValueChange = { viewModel.updateEventDescription(it) },
                label = { Text("説明") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // 期間（日付入力改善）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = event?.startDate ?: "",
                    onValueChange = { viewModel.updateEventStartDate(it) },
                    label = { Text("開始日") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("YYYY-MM-DD") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                    ),
                    visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
                )
                OutlinedTextField(
                    value = event?.endDate ?: "",
                    onValueChange = { viewModel.updateEventEndDate(it) },
                    label = { Text("終了日") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("YYYY-MM-DD") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                    ),
                    visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
                )
            }

            // 日付選択ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.showStartDatePicker() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("開始日を選択", fontSize = 12.sp)
                }
                
                Button(
                    onClick = { viewModel.showEndDatePicker() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("終了日を選択", fontSize = 12.sp)
                }
            }

            // Google Maps URL
            var mapUrlError by remember { mutableStateOf("") }
            val mapUrl = event?.mapUrl ?: ""
            OutlinedTextField(
                value = mapUrl,
                onValueChange = {
                    viewModel.updateEventMapUrl(it)
                    mapUrlError = if (it.isNotEmpty() && !(it.startsWith("http://") || it.startsWith("https://"))) {
                        "正しいURLを入力してください"
                    } else {
                        ""
                    }
                },
                label = { Text("Google Maps URL") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://maps.google.com/...") },
                isError = mapUrlError.isNotEmpty()
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
                        text = "権限設定",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 権限の説明
                    Text(
                        text = "• オーナー: 完全な管理権限\n• 編集者: 行事の編集可能\n• 閲覧者: 閲覧のみ\n• ゲスト: シリアルコードでアクセス",
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
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = guestAccess.serialCode,
                            onValueChange = { viewModel.updateGuestAccessSerialCode(it) },
                            label = { Text("シリアルコード") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("例: ABC123") }
                        )
                    }
                }
            }

            // オリジナルボタン設定
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

                    // ボタン1
                    OutlinedTextField(
                        value = event?.button1?.text ?: "",
                        onValueChange = { viewModel.updateButton1Text(it) },
                        label = { Text("ボタン1 テキスト") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var button1UrlError by remember { mutableStateOf("") }
                    val button1Url = event?.button1?.url ?: ""
                    OutlinedTextField(
                        value = button1Url,
                        onValueChange = {
                            viewModel.updateButton1Url(it)
                            button1UrlError = if (it.isNotEmpty() && !(it.startsWith("http://") || it.startsWith("https://"))) {
                                "正しいURLを入力してください"
                            } else {
                                ""
                            }
                        },
                        label = { Text("ボタン1 URL") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = button1UrlError.isNotEmpty()
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
                    OutlinedTextField(
                        value = event?.button1?.icon ?: "",
                        onValueChange = { viewModel.updateButton1Icon(it) },
                        label = { Text("ボタン1 アイコン") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例: Add, Settings, Share") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ボタン2
                    OutlinedTextField(
                        value = event?.button2?.text ?: "",
                        onValueChange = { viewModel.updateButton2Text(it) },
                        label = { Text("ボタン2 テキスト") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var button2UrlError by remember { mutableStateOf("") }
                    val button2Url = event?.button2?.url ?: ""
                    OutlinedTextField(
                        value = button2Url,
                        onValueChange = {
                            viewModel.updateButton2Url(it)
                            button2UrlError = if (it.isNotEmpty() && !(it.startsWith("http://") || it.startsWith("https://"))) {
                                "正しいURLを入力してください"
                            } else {
                                ""
                            }
                        },
                        label = { Text("ボタン2 URL") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = button2UrlError.isNotEmpty()
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
                    OutlinedTextField(
                        value = event?.button2?.icon ?: "",
                        onValueChange = { viewModel.updateButton2Icon(it) },
                        label = { Text("ボタン2 アイコン") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例: Add, Settings, Share") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ボタン3
                    OutlinedTextField(
                        value = event?.button3?.text ?: "",
                        onValueChange = { viewModel.updateButton3Text(it) },
                        label = { Text("ボタン3 テキスト") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var button3UrlError by remember { mutableStateOf("") }
                    val button3Url = event?.button3?.url ?: ""
                    OutlinedTextField(
                        value = button3Url,
                        onValueChange = {
                            viewModel.updateButton3Url(it)
                            button3UrlError = if (it.isNotEmpty() && !(it.startsWith("http://") || it.startsWith("https://"))) {
                                "正しいURLを入力してください"
                            } else {
                                ""
                            }
                        },
                        label = { Text("ボタン3 URL") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = button3UrlError.isNotEmpty()
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
                    OutlinedTextField(
                        value = event?.button3?.icon ?: "",
                        onValueChange = { viewModel.updateButton3Icon(it) },
                        label = { Text("ボタン3 アイコン") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例: Add, Settings, Share") }
                    )
                }
            }

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
                modifier = Modifier.fillMaxWidth()
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
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = member.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ID: ${member.personalId}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
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
                        .width(120.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
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