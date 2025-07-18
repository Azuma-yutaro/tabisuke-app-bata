package com.tabisuke.app.ui.createevent
import com.tabisuke.app.R

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    navController: NavController,
    groupId: String,
    viewModel: CreateEventViewModel = viewModel()
) {
    val eventTitle by viewModel.eventTitle.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val context = LocalContext.current
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // バリデーション状態
    var isStartDateValid by remember { mutableStateOf(true) }
    var isEndDateValid by remember { mutableStateOf(true) }
    var validationMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.setGroupId(groupId)
        // 開始日のデフォルト値を今日に設定
        if (startDate.isEmpty()) {
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            viewModel.onStartDateChange(today)
        }
    }
    
    // 日付バリデーション
    LaunchedEffect(startDate, endDate) {
        if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
            try {
                val start = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val end = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                
                if (end.isBefore(start)) {
                    isEndDateValid = false
                    validationMessage = "終了日は開始日以降にしてください"
                } else {
                    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end)
                    if (daysBetween > 30) {
                        isEndDateValid = false
                        validationMessage = "期間は最大30日までです"
                    } else {
                        isEndDateValid = true
                        validationMessage = ""
                    }
                }
            } catch (e: Exception) {
                // 日付パースエラーの場合はバリデーションをスキップ
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "イベント作成",
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = eventTitle,
                onValueChange = { 
                    if (it.length <= 20) {
                        viewModel.onEventTitleChange(it) 
                    }
                },
                label = { Text("イベント名 *") },
                placeholder = { Text("最大20文字") },
                modifier = Modifier.fillMaxWidth(),
                isError = eventTitle.isEmpty() && eventTitle.isNotEmpty(),
                supportingText = {
                    Text("${eventTitle.length}/20")
                }
            )

            OutlinedTextField(
                value = startDate,
                onValueChange = { },
                label = { Text("開始日 *") },
                placeholder = { Text("カレンダーから選択") },
                modifier = Modifier.fillMaxWidth(),
                isError = startDate.isEmpty() && startDate.isNotEmpty(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "カレンダーを開く"
                        )
                    }
                }
            )

            OutlinedTextField(
                value = endDate,
                onValueChange = { },
                label = { Text("終了日 *") },
                placeholder = { Text("カレンダーから選択") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isEndDateValid,
                readOnly = true,
                supportingText = {
                    if (validationMessage.isNotEmpty()) {
                        Text(validationMessage, color = MaterialTheme.colorScheme.error)
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { showEndDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "カレンダーを開く"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.createEvent(
                        onSuccess = { eventId ->
                            Toast.makeText(context, "イベントを作成しました！", Toast.LENGTH_SHORT).show()
                            navController.navigate("home/${groupId}/${eventId}") {
                                popUpTo("event_list/${groupId}") { inclusive = true }
                            }
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "イベント作成に失敗しました: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = eventTitle.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty() && isEndDateValid
            ) {
                Text("イベントを作成", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        
        // 開始日選択ダイアログ
        if (showStartDatePicker) {
            val today = LocalDate.now()
            val startDatePickerState = rememberDatePickerState(
                initialSelectedDateMillis = today.toEpochDay() * 24 * 60 * 60 * 1000
            )
            
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            startDatePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                                val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                viewModel.onStartDateChange(formattedDate)
                            }
                            showStartDatePicker = false 
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) {
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
        
        // 終了日選択ダイアログ
        if (showEndDatePicker) {
            val today = LocalDate.now()
            val endDatePickerState = rememberDatePickerState(
                initialSelectedDateMillis = today.toEpochDay() * 24 * 60 * 60 * 1000
            )
            
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            endDatePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                                val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                viewModel.onEndDateChange(formattedDate)
                            }
                            showEndDatePicker = false 
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) {
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
} 