package com.example.tabisuke.ui.createevent

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

    LaunchedEffect(Unit) {
        viewModel.setGroupId(groupId)
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
                onValueChange = { viewModel.onEventTitleChange(it) },
                label = { Text("イベント名") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = startDate,
                onValueChange = { viewModel.onStartDateChange(it) },
                label = { Text("開始日") },
                placeholder = { Text("例: 2024-01-15") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = endDate,
                onValueChange = { viewModel.onEndDateChange(it) },
                label = { Text("終了日") },
                placeholder = { Text("例: 2024-01-17") },
                modifier = Modifier.fillMaxWidth()
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
                enabled = eventTitle.isNotEmpty()
            ) {
                Text("イベントを作成", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
} 