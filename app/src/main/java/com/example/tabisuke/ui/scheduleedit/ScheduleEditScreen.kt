package com.example.tabisuke.ui.scheduleedit

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
fun ScheduleEditScreen(
    navController: NavController,
    groupId: String,
    eventId: String,
    viewModel: ScheduleEditViewModel = viewModel()
) {
    val date by viewModel.date.collectAsState()
    val time by viewModel.time.collectAsState()
    val title by viewModel.title.collectAsState()
    val budget by viewModel.budget.collectAsState()
    val url by viewModel.url.collectAsState()
    val image by viewModel.image.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.setGroupAndEventId(groupId, eventId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "行事登録",
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
            // 必須項目
            OutlinedTextField(
                value = date,
                onValueChange = { viewModel.onDateChange(it) },
                label = { Text("行く日にち *") },
                placeholder = { Text("例: 2024-01-15") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = time,
                onValueChange = { viewModel.onTimeChange(it) },
                label = { Text("時間 *") },
                placeholder = { Text("例: 12:00") },
                modifier = Modifier.fillMaxWidth()
            )

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
                onValueChange = { viewModel.onBudgetChange(it) },
                label = { Text("予算（円）") },
                placeholder = { Text("例: 1500") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = url,
                onValueChange = { viewModel.onUrlChange(it) },
                label = { Text("URL") },
                placeholder = { Text("例: https://example.com") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = image,
                onValueChange = { viewModel.onImageChange(it) },
                label = { Text("画像URL") },
                placeholder = { Text("例: https://example.com/image.jpg") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.saveSchedule(
                        onSuccess = {
                            Toast.makeText(context, "行事を登録しました！", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "登録に失敗しました: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = date.isNotEmpty() && time.isNotEmpty() && title.isNotEmpty()
            ) {
                Text("行事を登録", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
