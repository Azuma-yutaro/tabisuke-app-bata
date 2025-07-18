package com.tabisuke.app.ui.joingroup
import com.tabisuke.app.R

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
                    // 既にメンバーかどうかチェック
                    firestore.collection("groups")
                        .document(_groupId.value)
                        .collection("members")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { memberDoc ->
                            if (memberDoc.exists()) {
                                // 既にメンバーの場合、直接参加
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
                            } else {
                                // 既存のリクエストをチェック
                                firestore.collection("groups")
                                    .document(_groupId.value)
                                    .collection("join_requests")
                                    .document(userId)
                                    .get()
                                    .addOnSuccessListener { requestDoc ->
                                        if (requestDoc.exists()) {
                                            val status = requestDoc.getString("status")
                                            when (status) {
                                                "pending" -> _joinError.value = "既に参加リクエストを送信済みです。承認されるまでお待ちください。"
                                                "approved" -> {
                                                    // 承認済みの場合、メンバーとして追加してから参加
                                                    firestore.collection("groups")
                                                        .document(_groupId.value)
                                                        .collection("members")
                                                        .document(userId)
                                                        .set(
                                                            mapOf(
                                                                "display_name" to userName,
                                                                "role" to "member",
                                                                "joined_at" to com.google.firebase.Timestamp.now()
                                                            )
                                                        )
                                                        .addOnSuccessListener {
                                                            // イベント一覧に遷移
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
                                                        .addOnFailureListener { e -> _joinError.value = "メンバー追加に失敗しました: ${e.message}" }
                                                }
                                                "rejected" -> _joinError.value = "参加リクエストが拒否されました。"
                                                else -> _joinError.value = "不明なリクエスト状態です。"
                                            }
                                        } else {
                                            // 新しい参加リクエストを作成
                                            firestore.collection("groups")
                                                .document(_groupId.value)
                                                .collection("join_requests")
                                                .document(userId)
                                                .set(
                                                    mapOf(
                                                        "display_name" to userName,
                                                        "requested_at" to com.google.firebase.Timestamp.now(),
                                                        "status" to "pending"
                                                    )
                                                )
                                                .addOnSuccessListener {
                                                    _joinError.value = "参加リクエストを送信しました。承認されるまでお待ちください。"
                                                }
                                                .addOnFailureListener { e -> _joinError.value = "参加リクエストの送信に失敗しました: ${e.message}" }
                                        }
                                    }
                                    .addOnFailureListener { e -> _joinError.value = "リクエスト確認に失敗しました: ${e.message}" }
                            }
                        }
                        .addOnFailureListener { e -> _joinError.value = "メンバー確認に失敗しました: ${e.message}" }
                } else {
                    _joinError.value = "指定されたグループIDは存在しません。"
                }
            }
            .addOnFailureListener { e -> _joinError.value = "グループの確認に失敗しました: ${e.message}" }
    }
}
