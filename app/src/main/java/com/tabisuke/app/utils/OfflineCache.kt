package com.tabisuke.app.utils
import com.tabisuke.app.R

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OfflineCache {
    
    private const val PREF_NAME = "offline_cache"
    private const val KEY_GROUPS = "cached_groups"
    private const val KEY_EVENTS = "cached_events_"
    private const val KEY_SCHEDULES = "cached_schedules_"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"
    
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    
    private val _pendingOperations = MutableStateFlow<List<PendingOperation>>(emptyList())
    val pendingOperations: StateFlow<List<PendingOperation>> = _pendingOperations.asStateFlow()
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadPendingOperations()
    }
    
    // グループデータのキャッシュ
    fun cacheGroups(groups: List<Any>) {
        val json = gson.toJson(groups)
        prefs.edit().putString(KEY_GROUPS, json).apply()
        updateLastSync()
    }
    
    fun getCachedGroups(): List<Any>? {
        val json = prefs.getString(KEY_GROUPS, null) ?: return null
        return try {
            gson.fromJson(json, object : TypeToken<List<Any>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }
    
    // イベントデータのキャッシュ
    fun cacheEvents(groupId: String, events: List<Any>) {
        val json = gson.toJson(events)
        prefs.edit().putString("$KEY_EVENTS$groupId", json).apply()
        updateLastSync()
    }
    
    fun getCachedEvents(groupId: String): List<Any>? {
        val json = prefs.getString("$KEY_EVENTS$groupId", null) ?: return null
        return try {
            gson.fromJson(json, object : TypeToken<List<Any>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }
    
    // スケジュールデータのキャッシュ
    fun cacheSchedules(eventId: String, schedules: List<Any>) {
        val json = gson.toJson(schedules)
        prefs.edit().putString("$KEY_SCHEDULES$eventId", json).apply()
        updateLastSync()
    }
    
    fun getCachedSchedules(eventId: String): List<Any>? {
        val json = prefs.getString("$KEY_SCHEDULES$eventId", null) ?: return null
        return try {
            gson.fromJson(json, object : TypeToken<List<Any>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }
    
    // 保留中の操作を追加
    fun addPendingOperation(operation: PendingOperation) {
        val current = _pendingOperations.value.toMutableList()
        current.add(operation)
        _pendingOperations.value = current
        savePendingOperations()
    }
    
    // 保留中の操作を実行済みとして削除
    fun removePendingOperation(operation: PendingOperation) {
        val current = _pendingOperations.value.toMutableList()
        current.remove(operation)
        _pendingOperations.value = current
        savePendingOperations()
    }
    
    // 保留中の操作をすべて取得
    fun getPendingOperations(): List<PendingOperation> {
        return _pendingOperations.value
    }
    
    // 最後の同期時刻を更新
    private fun updateLastSync() {
        prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
    }
    
    // 最後の同期時刻を取得
    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }
    
    // 保留中の操作を保存
    private fun savePendingOperations() {
        val json = gson.toJson(_pendingOperations.value)
        prefs.edit().putString("pending_operations", json).apply()
    }
    
    // 保留中の操作を読み込み
    private fun loadPendingOperations() {
        val json = prefs.getString("pending_operations", "[]")
        try {
            val type = object : TypeToken<List<PendingOperation>>() {}.type
            val operations: List<PendingOperation> = gson.fromJson(json, type)
            _pendingOperations.value = operations
        } catch (e: Exception) {
            _pendingOperations.value = emptyList()
        }
    }
    
    // キャッシュをクリア
    fun clearCache() {
        prefs.edit().clear().apply()
        _pendingOperations.value = emptyList()
    }
}

// 保留中の操作を表すデータクラス
data class PendingOperation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: OperationType,
    val data: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OperationType {
    CREATE_GROUP,
    UPDATE_GROUP,
    DELETE_GROUP,
    CREATE_EVENT,
    UPDATE_EVENT,
    DELETE_EVENT,
    CREATE_SCHEDULE,
    UPDATE_SCHEDULE,
    DELETE_SCHEDULE
} 