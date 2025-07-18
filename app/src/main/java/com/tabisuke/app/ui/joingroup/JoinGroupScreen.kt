package com.tabisuke.app.ui.joingroup
import com.tabisuke.app.R

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    navController: NavController,
    viewModel: JoinGroupViewModel = viewModel()
) {
    val groupId by viewModel.groupId.collectAsState()
    val joinError by viewModel.joinError.collectAsState()
    val context = LocalContext.current
    
    // QRコード読み取り用のランチャー
    val qrScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scannedGroupId = result.data?.getStringExtra("group_id")
            if (scannedGroupId != null) {
                viewModel.onGroupIdChange(scannedGroupId)
                Toast.makeText(context, "QRコードを読み取りました: $scannedGroupId", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 確認ダイアログの状態
    var showConfirmDialog by remember { mutableStateOf(false) }
    var scannedGroupIdForConfirm by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "グループ参加",
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "共有するたび、絆になる。",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A4C93),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "私たちの旅は、ここから始まる",
                fontSize = 14.sp,
                color = Color(0xFF6A4C93).copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            // 入力欄上のラベル
            Text(
                text = "グループを検索",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "グループIDの確認…イベント詳細画面下部などに表示されているIDを共有してもらいましょう",
                fontSize = 10.sp,
                color = Color(0xFF333333).copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // QRコード読み取りボタン
            Button(
                onClick = {
                    val intent = android.content.Intent(context, com.tabisuke.app.ui.qrscanner.QRScannerActivity::class.java)
                    qrScannerLauncher.launch(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A4C93)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "QRコード読み取り",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("QRコードを読み取る", fontSize = 16.sp)
            }
            
            // またはのテキスト
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = "または",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Divider(modifier = Modifier.weight(1f))
            }
            
            OutlinedTextField(
                value = groupId,
                onValueChange = { viewModel.onGroupIdChange(it) },
                label = { Text("グループID") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF6A4C93))
            )
            
            // エラーメッセージ
            joinError?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // 参加ボタン
            Button(
                onClick = {
                    if (groupId.isNotBlank()) {
                        showConfirmDialog = true
                        scannedGroupIdForConfirm = groupId
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A4C93)
                ),
                enabled = groupId.isNotBlank()
            ) {
                Text("グループに参加", fontSize = 16.sp)
            }
        }
        
        // 確認ダイアログ
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = {
                    Text("グループ参加の確認")
                },
                text = {
                    Text("グループID「$scannedGroupIdForConfirm」のグループに参加しますか？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.joinGroup { groupId, eventId ->
                                navController.navigate("home/$groupId/$eventId") {
                                    popUpTo("join_group") { inclusive = true }
                                }
                            }
                        }
                    ) {
                        Text("参加する")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmDialog = false }
                    ) {
                        Text("キャンセル")
                    }
                }
            )
        }
    }
}
