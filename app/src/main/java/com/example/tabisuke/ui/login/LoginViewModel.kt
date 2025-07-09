package com.example.tabisuke.ui.login

import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

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

        auth.signInWithEmailAndPassword(_email.value, _password.value)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _loginError.value = task.exception?.message
                }
            }
    }

    fun signUp() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _loginError.value = "メールアドレスとパスワードを入力してください。"
            return
        }

        auth.createUserWithEmailAndPassword(_email.value, _password.value)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _loginError.value = task.exception?.message
                }
            }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _loginError.value = task.exception?.message
                }
            }
    }

    fun setLoginError(message: String?) {
        _loginError.value = message
    }
}
