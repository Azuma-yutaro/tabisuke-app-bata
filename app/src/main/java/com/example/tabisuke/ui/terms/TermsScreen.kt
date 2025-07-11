package com.example.tabisuke.ui.terms

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.Intent
import android.net.Uri

@Composable
fun TermsScreen(navController: NavController, prefs: SharedPreferences) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("利用規約・プライバシーポリシー", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Text("このアプリを利用するには、利用規約とプライバシーポリシーに同意していただく必要があります。", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            TextButton(onClick = {
                val url = "https://example.com/terms" // 仮URL
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }) {
                Text("利用規約を読む")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = {
                val url = "https://example.com/privacy" // 仮URL
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }) {
                Text("プライバシーポリシーを読む")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            prefs.edit().putBoolean("agreed_to_terms", true).apply()
            navController.navigate("welcome") {
                popUpTo("terms") { inclusive = true }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("同意して進む", fontSize = 18.sp)
        }
    }
} 