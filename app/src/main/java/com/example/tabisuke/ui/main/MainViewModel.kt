package com.example.tabisuke.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.tabisuke.ui.scheduledetail.Schedule
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.tabisuke.utils.ErrorHandler
import com.example.tabisuke.utils.NetworkUtils
import com.example.tabisuke.utils.OfflineCache
import com.example.tabisuke.utils.PendingOperation
import com.example.tabisuke.utils.OperationType
import com.example.tabisuke.utils.SyncManager

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val mapUrl: String,
    val button1: ButtonConfig,
    val button2: ButtonConfig,
    val button3: ButtonConfig
)

data class ButtonConfig(
    val text: String,
    val icon: String,
    val url: String
)

data class ScheduleWithDate(
    val schedule: Schedule,
    val actualDate: String
)

class MainViewModel : ViewModel() {
    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules

    private val _schedulesWithDates = MutableStateFlow<List<ScheduleWithDate>>(emptyList())
    val schedulesWithDates: StateFlow<List<ScheduleWithDate>> = _schedulesWithDates

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

    fun fetchEvent(groupId: String, eventId: String) {
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
                    fetchEventFromFirestore(groupId, eventId)
                    
                    // 接続復旧時は保留中の操作を同期
                    if (!_isOffline.value) {
                        syncPendingOperations()
                    }
                } else {
                    // オフライン時: キャッシュから取得
                    fetchEventFromCache(groupId, eventId)
                }
                
                // 保留中の操作数を更新
                updatePendingOperationsCount()
                
            } catch (e: Exception) {
                ErrorHandler.logError("MainViewModel", "イベントの取得に失敗", e)
                _errorMessage.value = ErrorHandler.getFirebaseErrorMessage(e)
                _hasError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun fetchEventFromFirestore(groupId: String, eventId: String) {
        val eventDoc = firestore.collection("groups")
            .document(groupId)
            .collection("events")
            .document(eventId)
            .get()
            .await()

        if (eventDoc.exists()) {
            val data = eventDoc.data!!
            val event = Event(
                id = eventId,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                startDate = data["startDate"] as? String ?: "",
                endDate = data["endDate"] as? String ?: "",
                mapUrl = data["mapUrl"] as? String ?: "",
                button1 = ButtonConfig(
                    text = data["button1Text"] as? String ?: "",
                    icon = data["button1Icon"] as? String ?: "",
                    url = data["button1Url"] as? String ?: ""
                ),
                button2 = ButtonConfig(
                    text = data["button2Text"] as? String ?: "",
                    icon = data["button2Icon"] as? String ?: "",
                    url = data["button2Url"] as? String ?: ""
                ),
                button3 = ButtonConfig(
                    text = data["button3Text"] as? String ?: "",
                    icon = data["button3Icon"] as? String ?: "",
                    url = data["button3Url"] as? String ?: ""
                )
            )
            _event.value = event
            
            // キャッシュに保存
            OfflineCache.cacheEvents(groupId, listOf(event))
        } else {
            _errorMessage.value = "イベントが見つかりません"
            _hasError.value = true
        }
    }
    
    private fun fetchEventFromCache(groupId: String, eventId: String) {
        val cachedEvents = OfflineCache.getCachedEvents(groupId)
        if (cachedEvents != null) {
            try {
                // キャッシュされたデータからイベントを検索
                // 実際の実装では、キャッシュされたデータの構造に合わせて調整が必要
                _event.value = null
            } catch (e: Exception) {
                _event.value = null
            }
        } else {
            _event.value = null
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
                    ErrorHandler.logError("MainViewModel", "同期に失敗", e)
                }
            )
        }
    }
    
    private fun updatePendingOperationsCount() {
        val pendingOps = OfflineCache.getPendingOperations()
        _pendingOperationsCount.value = pendingOps.size
    }

    fun loadSchedules(groupId: String, eventId: String) {
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
                    loadSchedulesFromFirestore(groupId, eventId)
                } else {
                    // オフライン時: キャッシュから取得
                    loadSchedulesFromCache(groupId, eventId)
                }
                
            } catch (e: Exception) {
                ErrorHandler.logError("MainViewModel", "スケジュールの読み込みに失敗", e)
                _errorMessage.value = ErrorHandler.getFirebaseErrorMessage(e)
                _hasError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadSchedulesFromFirestore(groupId: String, eventId: String) {
        val schedulesSnapshot = firestore.collection("groups")
            .document(groupId)
            .collection("events")
            .document(eventId)
            .collection("schedules")
            .get()
            .await()

        val schedulesList = schedulesSnapshot.documents.mapNotNull { doc ->
            val data = doc.data
            if (data != null) {
                Schedule(
                    id = doc.id,
                    dayNumber = (data["dayNumber"] as? Long)?.toInt() ?: 1,
                    time = data["time"] as? String ?: "",
                    title = data["title"] as? String ?: "",
                    budget = data["budget"] as? Long ?: 0L,
                    url = data["url"] as? String ?: "",
                    image = data["image"] as? String ?: ""
                )
            } else null
        }

        val sortedSchedules = schedulesList.sortedWith(compareBy({ it.dayNumber }, { it.time }))
        
        _schedules.value = sortedSchedules
        
        // 日付付きのスケジュールリストを生成
        val eventData = _event.value
        if (eventData != null && eventData.startDate.isNotEmpty()) {
            val schedulesWithDates = sortedSchedules.map { schedule ->
                ScheduleWithDate(
                    schedule = schedule,
                    actualDate = getDateFromDayNumber(schedule.dayNumber, eventData.startDate)
                )
            }
            _schedulesWithDates.value = schedulesWithDates
        }
        
        // キャッシュに保存
        OfflineCache.cacheSchedules(eventId, sortedSchedules)
    }
    
    private fun loadSchedulesFromCache(groupId: String, eventId: String) {
        val cachedSchedules = OfflineCache.getCachedSchedules(eventId)
        if (cachedSchedules != null) {
            try {
                // キャッシュされたデータをScheduleオブジェクトに変換
                // 実際の実装では、キャッシュされたデータの構造に合わせて調整が必要
                _schedules.value = emptyList()
                _schedulesWithDates.value = emptyList()
            } catch (e: Exception) {
                _schedules.value = emptyList()
                _schedulesWithDates.value = emptyList()
            }
        } else {
            _schedules.value = emptyList()
            _schedulesWithDates.value = emptyList()
        }
    }

    // 日数から実際の日付を計算
    private fun getDateFromDayNumber(dayNumber: Int, startDate: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val start = LocalDate.parse(startDate, formatter)
            val targetDate = start.plusDays((dayNumber - 1).toLong())
            targetDate.format(formatter)
        } catch (e: Exception) {
            "日付不明"
        }
    }

    // 日数から表示用の文字列を生成
    fun getDayDisplayText(dayNumber: Int): String {
        return "${dayNumber}日目"
    }

    fun clearError() {
        _errorMessage.value = null
        _hasError.value = false
    }

    // マイページ用: ユーザー情報取得
    fun getCurrentUserDisplayName(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName ?: "未設定"
    }

    // マイページ用: 所属グループ一覧取得（membersサブコレクション対応）
    fun fetchUserGroupsWithCreator(onResult: (List<Triple<String, String, String>>) -> Unit) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("groups").get()
            .addOnSuccessListener { snapshot ->
                val groupDocs = snapshot.documents
                val resultList = mutableListOf<Triple<String, String, String>>()
                if (groupDocs.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }
                var checked = 0
                for (doc in groupDocs) {
                    val groupId = doc.id
                    val groupName = doc.getString("name") ?: continue
                    val createdBy = doc.getString("created_by") ?: ""
                    firestore.collection("groups").document(groupId).collection("members").document(userId).get()
                        .addOnSuccessListener { memberDoc ->
                            if (memberDoc.exists()) {
                                resultList.add(Triple(groupId, groupName, createdBy))
                            }
                            checked++
                            if (checked == groupDocs.size) {
                                onResult(resultList)
                            }
                        }
                        .addOnFailureListener {
                            checked++
                            if (checked == groupDocs.size) {
                                onResult(resultList)
                            }
                        }
                }
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
} 