package com.tabisuke.app.ui.login
import com.tabisuke.app.R

import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun onEmailChange(email: String) {
        _email.value = email
    }

    fun onPasswordChange(password: String) {
        _password.value = password
    }

    fun login() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _loginError.value = "メールアドレスとパスワードを入力してください。"
            return
        }

        android.util.Log.d("LoginViewModel", "メールログイン開始: ${_email.value}")
        auth.signInWithEmailAndPassword(_email.value, _password.value)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("LoginViewModel", "メールログイン成功")
                    _isLoggedIn.value = true
                    _loginError.value = null
                } else {
                    val errorMessage = when (task.exception) {
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "メールアドレスまたはパスワードが正しくありません"
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "このメールアドレスは登録されていません"
                        else -> "ログインエラー: ${task.exception?.message}"
                    }
                    android.util.Log.e("LoginViewModel", "メールログイン失敗: ${task.exception?.message}")
                    _loginError.value = errorMessage
                }
            }
    }

    fun signUp() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _loginError.value = "メールアドレスとパスワードを入力してください。"
            return
        }

        android.util.Log.d("LoginViewModel", "新規登録開始: ${_email.value}")
        auth.createUserWithEmailAndPassword(_email.value, _password.value)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("LoginViewModel", "新規登録成功")
                    _isLoggedIn.value = true
                    _loginError.value = null
                } else {
                    val errorMessage = when (task.exception) {
                        is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "パスワードが弱すぎます（6文字以上）"
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "メールアドレスの形式が正しくありません"
                        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "このメールアドレスは既に使用されています"
                        else -> "登録エラー: ${task.exception?.message}"
                    }
                    android.util.Log.e("LoginViewModel", "新規登録失敗: ${task.exception?.message}")
                    _loginError.value = errorMessage
                }
            }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        android.util.Log.d("LoginViewModel", "Google認証開始: ${account.email}")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("LoginViewModel", "Google認証成功")
                    _isLoggedIn.value = true
                    _loginError.value = null
                } else {
                    android.util.Log.e("LoginViewModel", "Google認証失敗: ${task.exception?.message}")
                    _loginError.value = task.exception?.message
                }
            }
    }

    fun setLoginError(message: String?) {
        _loginError.value = message
    }
}
