package com.tabisuke.app.ui.guestaccess
import com.tabisuke.app.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabisuke.app.utils.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GuestAccessViewModel : ViewModel() {

    private val _serialCode = MutableStateFlow("")
    val serialCode: StateFlow<String> = _serialCode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _isAccessGranted = MutableStateFlow(false)
    val isAccessGranted: StateFlow<Boolean> = _isAccessGranted
    
    private val permissionManager = PermissionManager()

    fun onSerialCodeChange(code: String) {
        _serialCode.value = code
        _errorMessage.value = ""
    }

    fun verifySerialCode(groupId: String, eventId: String) {
        if (_serialCode.value.isBlank()) {
            _errorMessage.value = "シリアルコードを入力してください"
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = ""
        
        viewModelScope.launch {
            try {
                val hasAccess = permissionManager.checkSerialCodeAccess(groupId, eventId, _serialCode.value)
                
                if (hasAccess) {
                    _isAccessGranted.value = true
                } else {
                    _errorMessage.value = "シリアルコードが正しくありません"
                }
            } catch (e: Exception) {
                _errorMessage.value = "エラーが発生しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun resetState() {
        _serialCode.value = ""
        _errorMessage.value = ""
        _isAccessGranted.value = false
    }
}
