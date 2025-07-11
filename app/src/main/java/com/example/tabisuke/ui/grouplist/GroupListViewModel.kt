package com.example.tabisuke.ui.grouplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.tabisuke.utils.ErrorHandler
import com.example.tabisuke.utils.NetworkUtils
import com.example.tabisuke.utils.OfflineCache
import com.example.tabisuke.utils.PendingOperation
import com.example.tabisuke.utils.OperationType
import com.example.tabisuke.utils.SyncManager

data class Group(
    val id: String,
    val name: String,
    val memberCount: Int,
    val eventCount: Int,
    val createdBy: String,
    val createdAt: com.google.firebase.Timestamp,
    val imageUrl: String
)

class GroupListViewModel : ViewModel() {
    
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _currentUser = MutableStateFlow("")
    val currentUser: StateFlow<String> = _currentUser
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError
    
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline
    
    private val _pendingOperationsCount = MutableStateFlow(0)
    val pendingOperationsCount: StateFlow<Int> = _pendingOperationsCount
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    fun loadGroups() {
        val userId = auth.currentUser?.uid ?: return
        
        // 現在のユーザー名を取得
        val userName = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Unknown User"
        _currentUser.value = userName
        
        _isLoading.value = true
        _errorMessage.value = null
        _hasError.value = false
        
        // ネットワーク状態を確認
        val isOnline = NetworkUtils.isOnline.value
        _isOffline.value = !isOnline
        
        viewModelScope.launch {
            try {
                if (isOnline) {
                    // オンライン時: Firestoreから取得
                    loadGroupsFromFirestore(userId)
                    
                    // 接続復旧時は保留中の操作を同期
                    if (!_isOffline.value) {
                        syncPendingOperations()
                    }
                } else {
                    // オフライン時: キャッシュから取得
                    loadGroupsFromCache()
                }
                
                // 保留中の操作数を更新
                updatePendingOperationsCount()
                
            } catch (e: Exception) {
                ErrorHandler.logError("GroupListViewModel", "グループ一覧の読み込みに失敗", e)
                _errorMessage.value = ErrorHandler.getFirebaseErrorMessage(e)
                _hasError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun syncPendingOperations() {
        val pendingOps = OfflineCache.getPendingOperations()
        if (pendingOps.isNotEmpty()) {
            _isSyncing.value = true
            
            SyncManager.syncPendingOperations(
                onSuccess = {
                    _isSyncing.value = false
                    updatePendingOperationsCount()
                },
                onFailure = { e ->
                    _isSyncing.value = false
                    ErrorHandler.logError("GroupListViewModel", "同期に失敗", e)
                }
            )
        }
    }
    
    private suspend fun loadGroupsFromFirestore(userId: String) {
        // すべてのグループを取得し、ユーザーがメンバーかどうかを確認
        val groupsSnapshot = firestore.collection("groups")
            .get()
            .await()
        
        val groupList = mutableListOf<Group>()
        
        for (groupDoc in groupsSnapshot.documents) {
            val groupId = groupDoc.id
            
            // ユーザーがこのグループのメンバーかどうかを確認
            val memberDoc = firestore.collection("groups")
                .document(groupId)
                .collection("members")
                .document(userId)
                .get()
                .await()
            
            if (memberDoc.exists()) {
                val groupData = groupDoc.data
                
                if (groupData != null) {
                    // メンバー数を取得
                    val membersSnapshot = firestore.collection("groups")
                        .document(groupId)
                        .collection("members")
                        .get()
                        .await()
                    
                    // イベント数を取得
                    val eventsSnapshot = firestore.collection("groups")
                        .document(groupId)
                        .collection("events")
                        .get()
                        .await()
                    
                    val group = Group(
                        id = groupId,
                        name = groupData["name"] as? String ?: "",
                        memberCount = membersSnapshot.size(),
                        eventCount = eventsSnapshot.size(),
                        createdBy = groupData["created_by"] as? String ?: "",
                        createdAt = groupData["created_at"] as? com.google.firebase.Timestamp 
                            ?: com.google.firebase.Timestamp.now(),
                        imageUrl = groupData["imageUrl"] as? String ?: ""
                    )
                    
                    groupList.add(group)
                }
            }
        }
        
        _groups.value = groupList
        
        // キャッシュに保存
        OfflineCache.cacheGroups(groupList)
    }
    
    private fun loadGroupsFromCache() {
        val cachedGroups = OfflineCache.getCachedGroups()
        if (cachedGroups != null) {
            try {
                val groupList = cachedGroups.mapNotNull { groupData ->
                    // キャッシュされたデータをGroupオブジェクトに変換
                    // 実際の実装では、キャッシュされたデータの構造に合わせて調整が必要
                    null // 仮の実装
                }
                _groups.value = groupList
            } catch (e: Exception) {
                _groups.value = emptyList()
            }
        } else {
            _groups.value = emptyList()
        }
    }
    
    private fun updatePendingOperationsCount() {
        val pendingOps = OfflineCache.getPendingOperations()
        _pendingOperationsCount.value = pendingOps.size
    }
    
    fun clearError() {
        _errorMessage.value = null
        _hasError.value = false
    }
    
    fun logout(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                auth.signOut()
                onSuccess()
            } catch (e: Exception) {
                ErrorHandler.logError("GroupListViewModel", "ログアウトに失敗", e)
                onFailure(e)
            }
        }
    }
} 