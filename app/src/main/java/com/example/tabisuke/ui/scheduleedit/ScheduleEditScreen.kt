package com.example.tabisuke.ui.scheduleedit

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.tabisuke.ui.main.EventBottomNavBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditScreen(
    navController: NavController,
    groupId: String,
    eventId: String,
    scheduleId: String? = null, // 編集時はスケジュールIDを渡す
    viewModel: ScheduleEditViewModel = viewModel()
) {
    val date by viewModel.date.collectAsState()
    val time by viewModel.time.collectAsState()
    val title by viewModel.title.collectAsState()
    val budget by viewModel.budget.collectAsState()
    val url by viewModel.url.collectAsState()
    val image by viewModel.image.collectAsState()
    val dayOptions by viewModel.dayOptions.collectAsState()
    val eventStartDate by viewModel.eventStartDate.collectAsState()
    val eventEndDate by viewModel.eventEndDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    


    // ドロップダウン状態
    var dateExpanded by remember { mutableStateOf(false) }
    var hourExpanded by remember { mutableStateOf(false) }
    var minuteExpanded by remember { mutableStateOf(false) }

    // 画像選択
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageUriChange(it.toString()) }
    }

    val showDeleteConfirm = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.setGroupAndEventId(groupId, eventId)
        // 編集時は既存データを読み込み
        if (scheduleId != null) {
            viewModel.loadSchedule(scheduleId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (scheduleId != null) "行事編集" else "行事登録",
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
        if (isLoading) {
            // ローディング画面
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "更新中...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "しばらくお待ちください",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // イベント期間表示
                if (eventStartDate != null && eventEndDate != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "イベント期間",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${eventStartDate} 〜 ${eventEndDate}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // 必須項目
                // 日数選択ドロップダウン
                ExposedDropdownMenuBox(
                    expanded = dateExpanded,
                    onExpandedChange = { dateExpanded = !dateExpanded }
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("行く日にち *") },
                        placeholder = { Text("日数を選択してください") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = dateExpanded,
                        onDismissRequest = { dateExpanded = false }
                    ) {
                        dayOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.onDateChange(option)
                                    dateExpanded = false
                                }
                            )
                        }
                    }
                }

                // 時間選択（時間と分を分けてドロップダウン）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 時間選択
                    ExposedDropdownMenuBox(
                        expanded = hourExpanded,
                        onExpandedChange = { hourExpanded = !hourExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = time.split(":").getOrNull(0) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("時間 *") },
                            placeholder = { Text("時") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hourExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = hourExpanded,
                            onDismissRequest = { hourExpanded = false }
                        ) {
                            (0..23).forEach { hour ->
                                DropdownMenuItem(
                                    text = { Text("${hour}時") },
                                    onClick = {
                                        val currentMinute = time.split(":").getOrNull(1) ?: "00"
                                        viewModel.onTimeChange(
                                            "${
                                                hour.toString().padStart(2, '0')
                                            }:${currentMinute}"
                                        )
                                        hourExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 分選択
                    ExposedDropdownMenuBox(
                        expanded = minuteExpanded,
                        onExpandedChange = { minuteExpanded = !minuteExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = time.split(":").getOrNull(1) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("分 *") },
                            placeholder = { Text("分") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minuteExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = minuteExpanded,
                            onDismissRequest = { minuteExpanded = false }
                        ) {
                            (0..59 step 10).forEach { minute ->
                                DropdownMenuItem(
                                    text = { Text("${minute}分") },
                                    onClick = {
                                        val currentHour = time.split(":").getOrNull(0) ?: "00"
                                        viewModel.onTimeChange(
                                            "${currentHour}:${
                                                minute.toString().padStart(2, '0')
                                            }"
                                        )
                                        minuteExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    label = { Text("タイトル *") },
                    placeholder = { Text("例: 昼食") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 任意項目
                OutlinedTextField(
                    value = budget,
                    onValueChange = { input ->
                        // 数字のみ許可
                        val filtered = input.filter { it.isDigit() }
                        viewModel.onBudgetChange(filtered)
                    },
                    label = { Text("予算（円）") },
                    placeholder = { Text("例: 1500") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                var urlError by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = url,
                    onValueChange = { viewModel.onUrlChange(it); urlError = if (it.isNotEmpty() && !(it.startsWith("http://") || it.startsWith("https://"))) { "正しいURLを入力してください" } else { "" } },
                    label = { Text("URL") },
                    placeholder = { Text("例: https://example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = urlError.isNotEmpty()
                )
                if (urlError.isNotEmpty()) {
                    Text(
                        text = urlError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                    )
                }

                // 画像アップロード
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "画像",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (image.isNotEmpty()) {
                            // 画像が選択されている場合
                            Column {
                                // 画像プレビュー
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (image.startsWith("content://")) {
                                            // ローカル画像の場合
                                            Image(
                                                painter = rememberAsyncImagePainter(image),
                                                contentDescription = "選択された画像",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else if (image.isNotEmpty()) {
                                            // ネットワーク画像の場合
                                            AsyncImage(
                                                model = image,
                                                contentDescription = "選択された画像",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                onError = {
                                                    // エラー時の処理（必要に応じて）
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // 削除ボタン
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { viewModel.onImageChange("") }
                                    ) {
                                        Text("画像を削除")
                                    }
                                }
                            }
                        } else {
                            // 画像が選択されていない場合
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "画像を追加",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("画像を選択")
                            }
                        }
                    }
                }

                            Spacer(modifier = Modifier.height(16.dp))
            
            // 登録/更新ボタン
            Button(
                onClick = {
                    if (scheduleId != null) {
                        viewModel.updateSchedule(
                            scheduleId = scheduleId,
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "行事を更新しました！",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.popBackStack()
                            },
                            onFailure = { e ->
                                Toast.makeText(
                                    context,
                                    "更新に失敗しました: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    } else {
                        viewModel.saveSchedule(
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "行事を登録しました！",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // 画面遷移せず、同じ画面に留まる
                            },
                            onFailure = { e ->
                                Toast.makeText(
                                    context,
                                    "登録に失敗しました: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = date.isNotEmpty() && time.isNotEmpty() && title.isNotEmpty() && urlError.isEmpty()
            ) {
                Text(
                    if (scheduleId != null) "行事を更新" else "登録", 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Medium
                )
            }
            // ボタン下に余白を追加
            Spacer(modifier = Modifier.height(24.dp))

            // 編集時のみ削除ボタンを表示
            if (scheduleId != null) {
                TextButton(
                    onClick = { showDeleteConfirm.value = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        "行事を削除",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 削除確認ダイアログ
            if (showDeleteConfirm.value) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm.value = false },
                    title = {
                        Text("本当に削除しますか？", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text("この操作は取り消せません。\n削除してもよろしいですか？")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirm.value = false
                                viewModel.deleteSchedule(
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "行事を削除しました！",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navController.popBackStack()
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(
                                            context,
                                            "削除に失敗しました: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("削除する", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm.value = false }) {
                            Text("キャンセル")
                        }
                    }
                )
            }
            }
        }
    }
}
