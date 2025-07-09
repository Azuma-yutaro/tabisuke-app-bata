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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text("メールアドレス") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = { Text("パスワード") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.login() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ログイン")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.signUp() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("新規登録")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { 
                android.util.Log.d("LoginScreen", "Googleログインボタンタップ")
                googleSignInLauncher.launch(googleSignInClient.signInIntent) 
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Googleでログイン")
        }

        loginError?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}
