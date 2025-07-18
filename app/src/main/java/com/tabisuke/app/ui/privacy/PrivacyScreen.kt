package com.tabisuke.app.ui.privacy
import com.tabisuke.app.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.Context
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tabisuke_prefs", Context.MODE_PRIVATE) }
    
    // 初回起動判定（プライバシーポリシーに同意していない場合が初回起動）
    val agreedToPrivacy = prefs.getBoolean("agreed_to_privacy", false)
    val isFirstLaunch = !agreedToPrivacy
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "プライバシーポリシー",
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
            Text(
                text = "プライバシーポリシー",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "最終更新日: 2024年1月",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. 収集する情報",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "当アプリは以下の情報を収集する場合があります：\n" +
                                "• アカウント情報（Googleアカウント連携時）\n" +
                                "• アプリ使用状況データ（Firebase Analytics）\n" +
                                "• デバイス情報（クラッシュレポート用）",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "2. 情報の使用目的",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "収集した情報は以下の目的でのみ使用されます：\n" +
                                "• アプリの機能提供\n" +
                                "• サービスの改善\n" +
                                "• バグの修正\n" +
                                "• セキュリティの確保",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "3. 情報の共有",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "当アプリは、法律で要求される場合を除き、\n" +
                                "お客様の個人情報を第三者に提供することはありません。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "4. データの保存",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "データはFirebaseの安全なサーバーに保存され、\n" +
                                "適切なセキュリティ対策が施されています。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "5. お問い合わせ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "プライバシーポリシーに関するお問い合わせは、\n" +
                                "アプリ内のお問い合わせフォームからお願いします。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            val url = "https://tabisuke.click/app/privacy_policy.html"
                            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode("プライバシーポリシー", StandardCharsets.UTF_8.toString())
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
                        // プライバシーポリシーに同意したことを保存
                        prefs.edit().putBoolean("agreed_to_privacy", true).apply()
                        // Welcome画面に遷移
                        navController.navigate("welcome") {
                            popUpTo("privacy") { inclusive = true }
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