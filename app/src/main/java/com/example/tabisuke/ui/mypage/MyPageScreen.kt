package com.example.tabisuke.ui.mypage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tabisuke.ui.main.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    navController: NavController,
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val displayName = remember { viewModel.getCurrentUserDisplayName() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var groupList by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var showFirstConfirm by remember { mutableStateOf<String?>(null) }
    var showSecondConfirm by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchUserGroupsWithCreator {
            groupList = it
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("マイページ", fontSize = 18.sp) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ユーザー名", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Text(displayName, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text("所属グループ一覧", fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(modifier = Modifier.weight(1f, false)) {
                    items(groupList) { (groupId, groupName, createdBy) ->
                        val isOwner = createdBy == currentUserId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(groupName, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (!isOwner) {
                                Button(
                                    onClick = { showFirstConfirm = groupId },
                                    enabled = true
                                ) {
                                    Text("脱退")
                                }
                            } else {
                                Text("作成者", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.height(16.dp))
            // 下部ボタン
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { navController.navigate("usage") }, modifier = Modifier.fillMaxWidth()) { Text("使い方") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("terms") }, modifier = Modifier.fillMaxWidth()) { Text("利用規約") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("privacy") }, modifier = Modifier.fillMaxWidth()) { Text("プライバシーポリシー") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("contact") }, modifier = Modifier.fillMaxWidth()) { Text("お問い合わせ") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "ログアウト")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ログアウト", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
        // 1回目の確認ダイアログ
        if (showFirstConfirm != null) {
            AlertDialog(
                onDismissRequest = { showFirstConfirm = null },
                title = { Text("グループ脱退の確認") },
                text = { Text("本当にこのグループから脱退しますか？") },
                confirmButton = {
                    TextButton(onClick = {
                        showSecondConfirm = showFirstConfirm
                        showFirstConfirm = null
                    }) { Text("はい") }
                },
                dismissButton = {
                    TextButton(onClick = { showFirstConfirm = null }) { Text("いいえ") }
                }
            )
        }
        // 2回目の確認ダイアログ
        if (showSecondConfirm != null) {
            AlertDialog(
                onDismissRequest = { showSecondConfirm = null },
                title = { Text("最終確認") },
                text = { Text("この操作は取り消せません。本当に脱退しますか？") },
                confirmButton = {
                    TextButton(onClick = {
                        // Firestoreから自分を削除
                        val groupId = showSecondConfirm!!
                        val userId = currentUserId
                        FirebaseFirestore.getInstance().collection("groups")
                            .document(groupId)
                            .collection("members")
                            .document(userId)
                            .delete()
                            .addOnSuccessListener {
                                groupList = groupList.filterNot { it.first == groupId }
                            }
                        showSecondConfirm = null
                    }) { Text("脱退する") }
                },
                dismissButton = {
                    TextButton(onClick = { showSecondConfirm = null }) { Text("キャンセル") }
                }
            )
        }
    }
    // ログアウト確認ダイアログ
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("ログアウトの確認") },
            text = { Text("本当にログアウトしますか？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout(
                        onSuccess = {
                            android.widget.Toast.makeText(context, "ログアウトしました", android.widget.Toast.LENGTH_SHORT).show()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onFailure = { e: Exception ->
                            android.widget.Toast.makeText(context, "ログアウトに失敗しました: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                    showLogoutConfirm = false
                }) { Text("ログアウト") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("キャンセル") }
            }
        )
    }
} 