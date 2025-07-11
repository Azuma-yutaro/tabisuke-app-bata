package com.example.tabisuke.ui.eventlist

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

data class Event(
    val id: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val createdBy: String,
    val serialEnabled: Boolean,
    val serialCode: String
)

class EventListViewModel : ViewModel() {
    
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
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
    
    fun loadEvents(groupId: String) {
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
                    loadEventsFromFirestore(groupId)
                    
                    // 接続復旧時は保留中の操作を同期
                    if (!_isOffline.value) {
                        syncPendingOperations()
                    }
                } else {
                    // オフライン時: キャッシュから取得
                    loadEventsFromCache(groupId)
                }
                
                // 保留中の操作数を更新
                updatePendingOperationsCount()
                
            } catch (e: Exception) {
                ErrorHandler.logError("EventListViewModel", "イベント一覧の読み込みに失敗", e)
                _errorMessage.value = ErrorHandler.getFirebaseErrorMessage(e)
                _hasError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadEventsFromFirestore(groupId: String) {
        val eventsSnapshot = firestore.collection("groups")
            .document(groupId)
            .collection("events")
            .get()
            .await()
        
        val eventList = mutableListOf<Event>()
        
        for (eventDoc in eventsSnapshot.documents) {
            val eventId = eventDoc.id
            val eventData = eventDoc.data
            
            if (eventData != null) {
                val event = Event(
                    id = eventId,
                    title = eventData["title"] as? String ?: "",
                    startDate = eventData["startDate"] as? String ?: eventData["start_date"] as? String ?: "",
                    endDate = eventData["endDate"] as? String ?: eventData["end_date"] as? String ?: "",
                    createdBy = eventData["created_by"] as? String ?: "",
                    serialEnabled = eventData["guestAccessEnabled"] as? Boolean ?: false,
                    serialCode = eventData["guestAccessSerialCode"] as? String ?: ""
                )
                
                eventList.add(event)
            }
        }
        
        _events.value = eventList
        
        // キャッシュに保存
        OfflineCache.cacheEvents(groupId, eventList)
    }
    
    private fun loadEventsFromCache(groupId: String) {
        val cachedEvents = OfflineCache.getCachedEvents(groupId)
        if (cachedEvents != null) {
            try {
                val eventList = cachedEvents.mapNotNull { eventData ->
                    // キャッシュされたデータをEventオブジェクトに変換
                    // 実際の実装では、キャッシュされたデータの構造に合わせて調整が必要
                    null // 仮の実装
                }
                _events.value = eventList
            } catch (e: Exception) {
                _events.value = emptyList()
            }
        } else {
            _events.value = emptyList()
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
                    ErrorHandler.logError("EventListViewModel", "同期に失敗", e)
                }
            )
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
}
