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
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                viewModel.firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                viewModel.setLoginError(e.message)
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
            onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
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
