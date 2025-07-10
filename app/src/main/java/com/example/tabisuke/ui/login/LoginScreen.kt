package com.example.tabisuke.ui.login
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import com.example.tabisuke.R
import androidx.compose.foundation.background
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = viewModel()) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val context = LocalContext.current

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(com.example.tabisuke.R.string.default_web_client_id))
        .requestEmail()
        .build()

    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        android.util.Log.d("LoginScreen", "GoogleSignIn結果: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                android.util.Log.d("LoginScreen", "GoogleSignIn成功: ${account.email}")
                viewModel.firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                android.util.Log.e("LoginScreen", "GoogleSignIn ApiException: ${e.statusCode} - ${e.message}")
                val errorMessage = when (e.statusCode) {
                    com.google.android.gms.common.ConnectionResult.SIGN_IN_REQUIRED -> "サインインが必要です"
                    com.google.android.gms.common.ConnectionResult.NETWORK_ERROR -> "ネットワークエラー"
                    com.google.android.gms.common.ConnectionResult.SERVICE_DISABLED -> "Googleサービスが無効です"
                    com.google.android.gms.common.ConnectionResult.SERVICE_INVALID -> "Googleサービスが無効です"
                    com.google.android.gms.common.ConnectionResult.SERVICE_MISSING -> "Googleサービスが見つかりません"
                    com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Googleサービスを更新してください"
                    com.google.android.gms.common.ConnectionResult.SUCCESS -> "成功"
                    else -> "Googleログインエラー: ${e.message}"
                }
                viewModel.setLoginError(errorMessage)
            }
        } else {
            android.util.Log.e("LoginScreen", "GoogleSignInキャンセルまたはエラー: ${result.resultCode}")
            viewModel.setLoginError("Googleログインがキャンセルされました")
        }
    }

    // 認証成功時の画面遷移
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            android.util.Log.d("LoginScreen", "認証成功、画面遷移開始")
            try {
                navController.navigate("group_list") {
                    popUpTo("login") { inclusive = true }
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginScreen", "画面遷移エラー: ${e.message}")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景画像
        Image(
            painter = painterResource(id = R.drawable.back05),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "さあ、旅の準備を始めよう",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A4C93),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp),
                lineHeight = 32.sp
            )
            // Googleログイン画像ボタン
            Image(
                painter = painterResource(id = R.drawable.google_login),
                contentDescription = "Googleでログイン",
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(84.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
                    .clickable {
                        android.util.Log.d("LoginScreen", "Googleログインボタンタップ")
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                alignment = Alignment.Center
            )
            // 「または、」テキスト（Googleの下）
            Text(
                text = "または、",
                fontSize = 15.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("メールアドレス") },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
                    .align(Alignment.CenterHorizontally),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("パスワード") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
                    .align(Alignment.CenterHorizontally),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.login() },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text("ログイン", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.signUp() },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text("新規登録", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "パスワードを忘れる心配がないGoogleログインを推奨します。",
                fontSize = 11.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally)
            )
            loginError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
