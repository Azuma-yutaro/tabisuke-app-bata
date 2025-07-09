package com.example.tabisuke.ui.scheduledetail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ScheduleDetailModal(
    schedule: Schedule,
    onDismiss: () -> Unit,
    onUrlClick: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // ヘッダー
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "行事詳細",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "閉じる"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // URLがある場合
                if (schedule.url.isNotEmpty()) {
                    Button(
                        onClick = { 
                            onUrlClick(schedule.url)
                            uriHandler.openUri(schedule.url)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("URLを開く")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 画像がある場合
                if (schedule.image.isNotEmpty()) {
                    Button(
                        onClick = { 
                            uriHandler.openUri(schedule.image)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("画像を表示")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 予算がある場合
                if (schedule.budget > 0) {
                    Text(
                        text = "予算: ${schedule.budget}円",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 閉じるボタン
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("閉じる")
                }
            }
        }
    }
}

data class Schedule(
    val date: String,
    val time: String,
    val title: String,
    val budget: Long,
    val url: String,
    val image: String
) 