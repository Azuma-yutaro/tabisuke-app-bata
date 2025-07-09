package com.example.tabisuke.ui.grouplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Group(
    val id: String,
    val name: String,
    val memberCount: Int,
    val createdBy: String,
    val createdAt: com.google.firebase.Timestamp
)

class GroupListViewModel : ViewModel() {
    
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    fun loadGroups() {
        val userId = auth.currentUser?.uid ?: return
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
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
                            
                            val group = Group(
                                id = groupId,
                                name = groupData["name"] as? String ?: "",
                                memberCount = membersSnapshot.size(),
                                createdBy = groupData["created_by"] as? String ?: "",
                                createdAt = groupData["created_at"] as? com.google.firebase.Timestamp 
                                    ?: com.google.firebase.Timestamp.now()
                            )
                            
                            groupList.add(group)
                        }
                    }
                }
                
                _groups.value = groupList
            } catch (e: Exception) {
                // エラーハンドリング
            } finally {
                _isLoading.value = false
            }
        }
    }
} 