package com.tabisuke.app.utils
import com.tabisuke.app.R

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object SyncManager {
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    fun syncPendingOperations(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val pendingOps = OfflineCache.getPendingOperations()
        if (pendingOps.isEmpty()) {
            onSuccess()
            return
        }
        
        _isSyncing.value = true
        _syncProgress.value = 0f
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var completedCount = 0
                
                for (operation in pendingOps) {
                    when (operation.type) {
                        OperationType.CREATE_GROUP -> {
                            syncCreateGroup(operation)
                        }
                        OperationType.UPDATE_GROUP -> {
                            syncUpdateGroup(operation)
                        }
                        OperationType.DELETE_GROUP -> {
                            syncDeleteGroup(operation)
                        }
                        OperationType.CREATE_EVENT -> {
                            syncCreateEvent(operation)
                        }
                        OperationType.UPDATE_EVENT -> {
                            syncUpdateEvent(operation)
                        }
                        OperationType.DELETE_EVENT -> {
                            syncDeleteEvent(operation)
                        }
                        OperationType.CREATE_SCHEDULE -> {
                            syncCreateSchedule(operation)
                        }
                        OperationType.UPDATE_SCHEDULE -> {
                            syncUpdateSchedule(operation)
                        }
                        OperationType.DELETE_SCHEDULE -> {
                            syncDeleteSchedule(operation)
                        }
                    }
                    
                    completedCount++
                    _syncProgress.value = completedCount.toFloat() / pendingOps.size
                    
                    // 操作を削除
                    OfflineCache.removePendingOperation(operation)
                }
                
                _lastSyncTime.value = System.currentTimeMillis()
                _isSyncing.value = false
                onSuccess()
                
            } catch (e: Exception) {
                _isSyncing.value = false
                onFailure(e)
            }
        }
    }
    
    private suspend fun syncCreateGroup(operation: PendingOperation) {
        val groupData = operation.data
        val groupId = groupData["id"] as? String ?: return
        
        firestore.collection("groups")
            .document(groupId)
            .set(groupData)
            .await()
    }
    
    private suspend fun syncUpdateGroup(operation: PendingOperation) {
        val groupData = operation.data
        val groupId = groupData["id"] as? String ?: return
        
        firestore.collection("groups")
            .document(groupId)
            .update(groupData)
            .await()
    }
    
    private suspend fun syncDeleteGroup(operation: PendingOperation) {
        val groupId = operation.data["id"] as? String ?: return
        
        firestore.collection("groups")
            .document(groupId)
            .delete()
            .await()
    }
    
    private suspend fun syncCreateEvent(operation: PendingOperation) {
        val eventData = operation.data
        val groupId = eventData["groupId"] as? String ?: return
        val eventId = eventData["id"] as? String ?: return
        
        firestore.collection("groups")
            .document(groupId)
            .collection("events")
            .document(eventId)
            .set(eventData)
            .await()
    }
    
    private suspend fun syncUpdateEvent(operation: PendingOperation) {
        val eventData = operation.data
        val groupId = eventData["groupId"] as? String ?: return
        val eventId = eventData["id"] as? String ?: return
        
        firestore.collection("groups")
            .document(groupId)
            .collection("events")
            .document(eventId)
            .update(eventData)
            .await()
    }
    
    private suspend fun syncDeleteEvent(operation: PendingOperation) {
        val groupId = operation.data["groupId"] as? String ?: return
        val eventId = operation.data["id"] as? String ?: return
        
        firestore.collection("groups")
            .document(groupId)
            .collection("events")
            .document(eventId)
            .delete()
            .await()
    }
    
    private suspend fun syncCreateSchedule(operation: PendingOperation) {
        val scheduleData = operation.data
        val groupId = scheduleData["groupId"] as? String ?: return
        val eventId = scheduleData["eventId"] as? String ?: return
        val scheduleId = scheduleData["id"] as? String ?: return
        
        firestore.collection("groups")
            .document(groupId)
            .collection("events")
            .document(eventId)
            .collection("schedules")
            .document(scheduleId)
            .set(scheduleData)
            .await()
    }
    
    private suspend fun syncUpdateSchedule(operation: PendingOperation) {
        val scheduleData = operation.data
        val groupId = scheduleData["groupId"] as? String ?: return
        val eventId = scheduleData["eventId"] as? String ?: return
        val scheduleId = scheduleData["id"] as? String ?: return
        
        firestore.collection("groups")
            .document(groupId)
            .collection("events")
            .document(eventId)
            .collection("schedules")
            .document(scheduleId)
            .update(scheduleData)
            .await()
    }
    
    private suspend fun syncDeleteSchedule(operation: PendingOperation) {
        val groupId = operation.data["groupId"] as? String ?: return
        val eventId = operation.data["eventId"] as? String ?: return
        val scheduleId = operation.data["id"] as? String ?: return
        
        firestore.collection("groups")
            .document(groupId)
            .collection("events")
            .document(eventId)
            .collection("schedules")
            .document(scheduleId)
            .delete()
            .await()
    }
} 