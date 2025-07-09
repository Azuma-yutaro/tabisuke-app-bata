package com.example.tabisuke.ui.joingroup

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class JoinGroupViewModel : ViewModel() {

    private val _groupId = MutableStateFlow("")
    val groupId: StateFlow<String> = _groupId

    private val _joinError = MutableStateFlow<String?>(null)
    val joinError: StateFlow<String?> = _joinError

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun onGroupIdChange(id: String) {
        _groupId.value = id
    }

    fun joinGroup(onSuccess: (groupId: String, eventId: String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { _joinError.value = "User not logged in"; return }
        val userName = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Unknown User"

        if (_groupId.value.isBlank()) {
            _joinError.value = "グループIDを入力してください。"
            return
        }

        firestore.collection("groups").document(_groupId.value).get()
            .addOnSuccessListener { groupDoc ->
                if (groupDoc.exists()) {
                    // Add user to group members
                    val membersMap = groupDoc.get("members") as? MutableMap<String, Map<String, String>> ?: mutableMapOf()
                    membersMap[userId] = mapOf("name" to userName, "role" to "viewer") // Default role is viewer

                    firestore.collection("groups").document(_groupId.value).update("members", membersMap)
                        .addOnSuccessListener {
                            // Find the first event in the group to navigate to
                            firestore.collection("groups").document(_groupId.value).collection("events").limit(1).get()
                                .addOnSuccessListener { eventQuery ->
                                    if (!eventQuery.isEmpty) {
                                        val eventId = eventQuery.documents[0].id
                                        onSuccess(_groupId.value, eventId)
                                    } else {
                                        _joinError.value = "グループにイベントがありません。"
                                    }
                                }
                                .addOnFailureListener { e -> _joinError.value = "イベントの取得に失敗しました: ${e.message}" }
                        }
                        .addOnFailureListener { e -> _joinError.value = "グループへの参加に失敗しました: ${e.message}" }
                } else {
                    _joinError.value = "指定されたグループIDは存在しません。"
                }
            }
            .addOnFailureListener { e -> _joinError.value = "グループの確認に失敗しました: ${e.message}" }
    }
}
