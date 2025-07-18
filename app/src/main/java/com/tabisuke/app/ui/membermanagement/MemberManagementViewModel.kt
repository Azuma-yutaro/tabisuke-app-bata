package com.tabisuke.app.ui.membermanagement
import com.tabisuke.app.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MemberInfo(
    val userId: String,
    val displayName: String,
    val role: String,
    val joinedAt: com.google.firebase.Timestamp
)

data class JoinRequest(
    val userId: String,
    val displayName: String,
    val requestedAt: com.google.firebase.Timestamp,
    val status: String
)

class MemberManagementViewModel : ViewModel() {
    
    private val _members = MutableStateFlow<List<MemberInfo>>(emptyList())
    val members: StateFlow<List<MemberInfo>> = _members
    
    private val _joinRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val joinRequests: StateFlow<List<JoinRequest>> = _joinRequests
    
    private val _currentUserRole = MutableStateFlow("")
    val currentUserRole: StateFlow<String> = _currentUserRole
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }
    
    fun loadMembers(groupId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // メンバー一覧を取得
                val membersSnapshot = firestore.collection("groups")
                    .document(groupId)
                    .collection("members")
                    .get()
                    .await()
                
                val membersList = mutableListOf<MemberInfo>()
                for (memberDoc in membersSnapshot.documents) {
                    val memberData = memberDoc.data
                    if (memberData != null) {
                        membersList.add(
                            MemberInfo(
                                userId = memberDoc.id,
                                displayName = memberData["display_name"] as? String ?: "不明",
                                role = memberData["role"] as? String ?: "member",
                                joinedAt = memberData["joined_at"] as? com.google.firebase.Timestamp 
                                    ?: com.google.firebase.Timestamp.now()
                            )
                        )
                    }
                }
                
                _members.value = membersList
                
                // 参加リクエストを取得
                val requestsSnapshot = firestore.collection("groups")
                    .document(groupId)
                    .collection("join_requests")
                    .whereEqualTo("status", "pending")
                    .get()
                    .await()
                
                val requestsList = mutableListOf<JoinRequest>()
                for (requestDoc in requestsSnapshot.documents) {
                    val requestData = requestDoc.data
                    if (requestData != null) {
                        requestsList.add(
                            JoinRequest(
                                userId = requestDoc.id,
                                displayName = requestData["display_name"] as? String ?: "不明",
                                requestedAt = requestData["requested_at"] as? com.google.firebase.Timestamp 
                                    ?: com.google.firebase.Timestamp.now(),
                                status = requestData["status"] as? String ?: "pending"
                            )
                        )
                    }
                }
                
                _joinRequests.value = requestsList
                
                // 現在のユーザーの役割を取得
                val currentUserId = auth.currentUser?.uid
                if (currentUserId != null) {
                    val memberDoc = firestore.collection("groups")
                        .document(groupId)
                        .collection("members")
                        .document(currentUserId)
                        .get()
                        .await()
                    
                    if (memberDoc.exists()) {
                        _currentUserRole.value = memberDoc.getString("role") ?: "member"
                    }
                }
                
            } catch (e: Exception) {
                // エラーハンドリング
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun approveJoinRequest(groupId: String, userId: String, displayName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // メンバーとして追加
                firestore.collection("groups")
                    .document(groupId)
                    .collection("members")
                    .document(userId)
                    .set(
                        mapOf(
                            "display_name" to displayName,
                            "role" to "member",
                            "joined_at" to com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()
                
                // 参加リクエストを承認済みに更新
                firestore.collection("groups")
                    .document(groupId)
                    .collection("join_requests")
                    .document(userId)
                    .update("status", "approved")
                    .await()
                
                // ローカル状態を更新
                loadMembers(groupId)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "参加リクエストの承認に失敗しました")
            }
        }
    }
    
    fun rejectJoinRequest(groupId: String, userId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // 参加リクエストを拒否済みに更新
                firestore.collection("groups")
                    .document(groupId)
                    .collection("join_requests")
                    .document(userId)
                    .update("status", "rejected")
                    .await()
                
                // ローカル状態を更新
                loadMembers(groupId)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "参加リクエストの拒否に失敗しました")
            }
        }
    }
    
    fun removeMember(groupId: String, userId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // メンバーを削除
                firestore.collection("groups")
                    .document(groupId)
                    .collection("members")
                    .document(userId)
                    .delete()
                    .await()
                
                // 参加リクエストも削除（存在する場合）
                firestore.collection("groups")
                    .document(groupId)
                    .collection("join_requests")
                    .document(userId)
                    .delete()
                    .await()
                
                // ローカル状態を更新
                loadMembers(groupId)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "メンバーの削除に失敗しました")
            }
        }
    }
} 