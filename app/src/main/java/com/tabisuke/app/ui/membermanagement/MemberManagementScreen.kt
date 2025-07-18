package com.tabisuke.app.ui.membermanagement
import com.tabisuke.app.R

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementScreen(
    navController: NavController,
    groupId: String,
    viewModel: MemberManagementViewModel = viewModel()
) {
    val members by viewModel.members.collectAsState()
    val joinRequests by viewModel.joinRequests.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId) {
        viewModel.loadMembers(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "メンバー管理",
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 承認済みメンバー一覧
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "承認済みメンバー (${members.size}人)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "メンバー",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (members.isEmpty()) {
                                Text(
                                    text = "メンバーがいません",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    members.forEach { member ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "メンバー",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = member.displayName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "役割: ${getRoleDisplayName(member.role)} | 参加日: ${formatDate(member.joinedAt)}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                            
                                            // 自分以外のメンバーを削除可能
                                            if (member.userId != viewModel.getCurrentUserId() && 
                                                (currentUserRole == "owner" || currentUserRole == "admin")) {
                                                IconButton(
                                                    onClick = { showDeleteConfirm = member.userId }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "削除",
                                                        tint = MaterialTheme.colorScheme.error
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
                
                // 参加リクエスト一覧（オーナーまたは管理者のみ）
                if (joinRequests.isNotEmpty() && (currentUserRole == "owner" || currentUserRole == "admin")) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "参加リクエスト (${joinRequests.size}件)",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "リクエスト",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    joinRequests.forEach { request ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "→",
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = request.displayName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "リクエスト日: ${formatDate(request.requestedAt)}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                            
                                            // 承認ボタン
                                            IconButton(
                                                onClick = {
                                                    viewModel.approveJoinRequest(
                                                        groupId = groupId,
                                                        userId = request.userId,
                                                        displayName = request.displayName,
                                                        onSuccess = {
                                                            Toast.makeText(context, "${request.displayName}を承認しました", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onFailure = { error ->
                                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                        }
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = "承認",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            
                                            // 拒否ボタン
                                            IconButton(
                                                onClick = {
                                                    viewModel.rejectJoinRequest(
                                                        groupId = groupId,
                                                        userId = request.userId,
                                                        onSuccess = {
                                                            Toast.makeText(context, "${request.displayName}を拒否しました", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onFailure = { error ->
                                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                        }
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "拒否",
                                                    tint = MaterialTheme.colorScheme.error
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
        }
    }
    
    // メンバー削除確認ダイアログ
    showDeleteConfirm?.let { userId ->
        val member = members.find { it.userId == userId }
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = {
                Text(
                    text = "メンバー削除の確認",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "${member?.displayName ?: "このメンバー"}をグループから削除しますか？\n\n削除すると、このメンバーはグループにアクセスできなくなります。",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(
                            groupId = groupId,
                            userId = userId,
                            onSuccess = {
                                Toast.makeText(context, "${member?.displayName ?: "メンバー"}を削除しました", Toast.LENGTH_SHORT).show()
                                showDeleteConfirm = null
                            },
                            onFailure = { error ->
                                Toast.makeText(context, "削除に失敗しました: $error", Toast.LENGTH_LONG).show()
                                showDeleteConfirm = null
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
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

private fun getRoleDisplayName(role: String): String {
    return when (role) {
        "owner" -> "オーナー"
        "admin" -> "管理者"
        "member" -> "メンバー"
        else -> "メンバー"
    }
}

private fun formatDate(timestamp: com.google.firebase.Timestamp): String {
    val date = Date(timestamp.seconds * 1000)
    val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return formatter.format(date)
} 