package com.tabisuke.app.ui.welcome
import com.tabisuke.app.R

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun WelcomeScreen(navController: NavController, prefs: SharedPreferences) {
    // パステルグラデーション背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF4E6), // パステルオレンジ
                        Color(0xFFE6F7FF), // パステルブルー
                        Color(0xFFFFE6F7)  // パステルピンク
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ロゴ風アイコン
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "tabisukeロゴ",
                    tint = Color(0xFFFF8C00),
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            // キャッチコピー
            Text(
                "共有するたび、\n絆になる。\n私たちの旅は、\nここから始まる",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF444444),
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            // サブ説明
            Text(
                "tabisukeはイベントや旅行のスケジュール・グループ管理をサポートするアプリです。\n\nグループを作成して、みんなで予定を共有しましょう！",
                fontSize = 15.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    prefs.edit().putBoolean("completed_welcome", true).apply()
                    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
                    navController.navigate(if (isLoggedIn) "group_list" else "login") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00))
            ) {
                Text("はじめる", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
} 