package com.tabisuke.app.ui.terms
import com.tabisuke.app.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.navigation.NavController
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tabisuke_prefs", Context.MODE_PRIVATE) }
    
    // 初回起動判定（利用規約に同意していない場合が初回起動）
    val agreedToTerms = prefs.getBoolean("agreed_to_terms", false)
    val isFirstLaunch = !agreedToTerms
    
    // ログイン状態判定
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "利用規約",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    // 初回起動時は戻るボタンを非表示
                    if (!isFirstLaunch) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "戻る"
                            )
                        }
                    }
                }
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
            // 利用規約の内容
            Text(
                text = "利用規約",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "tabisukeアプリの利用規約です。",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 詳細をWebViewで表示するボタン
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
                        text = "こちらからご確認ください。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val url = "https://tabisuke.click/app/terms.html"
                            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode("利用規約", StandardCharsets.UTF_8.toString())
                            navController.navigate("webview/$encodedUrl/$encodedTitle")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = "詳細を確認",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
            
            // 初回起動時のみ同意ボタンを表示
            if (isFirstLaunch) {
                Spacer(modifier = Modifier.weight(1f))
                
                // 同意ボタン
                Button(
                    onClick = {
                        // 利用規約に同意したことを保存
                        prefs.edit().putBoolean("agreed_to_terms", true).apply()
                        // プライバシーポリシー画面に遷移
                        navController.navigate("privacy") {
                            popUpTo("terms") { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "同意する",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
} 