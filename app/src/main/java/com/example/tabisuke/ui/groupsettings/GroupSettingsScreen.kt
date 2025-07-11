package com.example.tabisuke.ui.groupsettings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import kotlin.math.absoluteValue
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.example.tabisuke.utils.QRCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    navController: NavController,
    groupId: String,
    viewModel: GroupSettingsViewModel = viewModel()
) {
    val group by viewModel.group.collectAsState()
    val members by viewModel.members.collectAsState()
    val joinRequests by viewModel.joinRequests.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    var showEditDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "グループ設定",
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
                item {
                    // グループ画像
                    val pastelColors = listOf(
                        Color(0xFFFFF4E6), Color(0xFFE6F7FF), Color(0xFFE6FFF4),
                        Color(0xFFF4E6FF), Color(0xFFFFE6F7), Color(0xFFFFFBE6)
                    )
                    val bgColor = pastelColors[(group?.id?.hashCode()?.absoluteValue ?: 0) % pastelColors.size]
                    var uploading by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    val imagePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            uploading = true
                            viewModel.uploadGroupImage(
                                groupId = groupId,
                                imageUri = it,
                                context = context,
                                onSuccess = { uploading = false; Toast.makeText(context, "画像をアップロードしました", Toast.LENGTH_SHORT).show() },
                                onFailure = { msg -> uploading = false; Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (group?.imageUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = group?.imageUrl,
                                contentDescription = "グループ画像",
                                modifier = Modifier.size(80.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(80.dp).clip(CircleShape).background(bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = group?.name?.firstOrNull()?.toString() ?: "?",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                enabled = !uploading
                            ) {
                                Text(if (uploading) "アップロード中..." else "画像を選択")
                            }
                            if (group?.imageUrl?.isNotEmpty() == true) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        uploading = true
                                        viewModel.deleteGroupImage(
                                            groupId = groupId,
                                            onSuccess = { uploading = false; Toast.makeText(context, "画像を削除しました", Toast.LENGTH_SHORT).show() },
                                            onFailure = { msg -> uploading = false; Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                        )
                                    },
                                    enabled = !uploading
                                ) {
                                    Text("画像を削除")
                                }
                            }
                        }
                    }
                }
                item {
                    // グループ名
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
                                    text = "グループ名",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                
                                IconButton(
                                    onClick = {
                                        newGroupName = group?.name ?: ""
                                        showEditDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "編集",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = group?.name ?: "読み込み中...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                item {
                    // グループID
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
                                text = "グループID",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = groupId,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("グループID", groupId)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "グループIDをコピーしました", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = "コピー",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            
                            // QRコード表示
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "QRコード",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val qrBitmap = remember(groupId) {
                                QRCodeGenerator.generateQRCode(groupId, 200)
                            }
                            
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "グループIDのQRコード",
                                    modifier = Modifier.size(200.dp)
                                )
                            }
                        }
                    }
                }
                
                item {
                    // メンバー一覧
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
                                    text = "メンバー一覧 (${members.size}人)",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                
                                // オーナーまたは管理者のみメンバー管理画面にアクセス可能
                                if (currentUserRole == "owner" || currentUserRole == "admin") {
                                    TextButton(
                                        onClick = { navController.navigate("member_management/$groupId") }
                                    ) {
                                        Text(
                                            text = "詳細管理",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
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
                                                    text = "役割: ${getRoleDisplayName(member.role)}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                            Text(
                                                text = formatDate(member.joinedAt),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 参加リクエストがある場合のみ表示（オーナーまたは管理者のみ）
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
                                Text(
                                    text = "参加リクエスト (${joinRequests.size}件)",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    joinRequests.forEach { request ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "リクエスト",
                                                tint = MaterialTheme.colorScheme.primary,
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
                                                    imageVector = Icons.Default.Check,
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
                                                    imageVector = Icons.Default.Close,
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
    
    // グループ名編集ダイアログ
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("グループ名を編集") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { 
                        if (it.length <= 15) {
                            newGroupName = it 
                        }
                    },
                    label = { Text("グループ名") },
                    placeholder = { Text("最大15文字") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("${newGroupName.length}/15")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            viewModel.updateGroupName(
                                groupId = groupId,
                                newName = newGroupName,
                                onSuccess = {
                                    Toast.makeText(context, "グループ名を更新しました", Toast.LENGTH_SHORT).show()
                                    showEditDialog = false
                                },
                                onFailure = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
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