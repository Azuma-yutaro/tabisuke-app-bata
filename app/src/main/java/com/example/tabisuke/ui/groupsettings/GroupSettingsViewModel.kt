package com.example.tabisuke.ui.groupsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.content.Context

data class GroupInfo(
    val id: String,
    val name: String,
    val memberCount: Int,
    val createdBy: String,
    val createdAt: com.google.firebase.Timestamp,
    val imageUrl: String = ""
)

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
    val status: String // "pending", "approved", "rejected"
)

class GroupSettingsViewModel : ViewModel() {
    
    private val _group = MutableStateFlow<GroupInfo?>(null)
    val group: StateFlow<GroupInfo?> = _group
    
    private val _members = MutableStateFlow<List<MemberInfo>>(emptyList())
    val members: StateFlow<List<MemberInfo>> = _members
    
    private val _joinRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val joinRequests: StateFlow<List<JoinRequest>> = _joinRequests
    
    private val _currentUserRole = MutableStateFlow<String?>(null)
    val currentUserRole: StateFlow<String?> = _currentUserRole
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    
    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val groupDoc = firestore.collection("groups")
                    .document(groupId)
                    .get()
                    .await()

                if (groupDoc.exists()) {
                    val data = groupDoc.data!!
                    
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
                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUserId != null) {
                        val currentUserMemberDoc = firestore.collection("groups")
                            .document(groupId)
                            .collection("members")
                            .document(currentUserId)
                            .get()
                            .await()
                        
                        if (currentUserMemberDoc.exists()) {
                            _currentUserRole.value = currentUserMemberDoc.getString("role") ?: "member"
                        } else {
                            _currentUserRole.value = null
                        }
                    }
                    
                    _group.value = GroupInfo(
                        id = groupId,
                        name = data["name"] as? String ?: "",
                        memberCount = membersList.size,
                        createdBy = data["created_by"] as? String ?: "",
                        createdAt = data["created_at"] as? com.google.firebase.Timestamp 
                            ?: com.google.firebase.Timestamp.now(),
                        imageUrl = data["imageUrl"] as? String ?: ""
                    )
                }
            } catch (e: Exception) {
                // エラーハンドリング
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateGroupName(groupId: String, newName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("groups")
                    .document(groupId)
                    .update("name", newName)
                    .await()
                
                // ローカル状態も更新
                _group.value = _group.value?.copy(name = newName)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "グループ名の更新に失敗しました")
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
                loadGroup(groupId)
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
                loadGroup(groupId)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "参加リクエストの拒否に失敗しました")
            }
        }
    }

    // グループ画像アップロード
    fun uploadGroupImage(groupId: String, imageUri: Uri, context: Context, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // 画像バリデーション
                val validationResult = validateImage(imageUri, context)
                if (!validationResult.isValid) {
                    onFailure(validationResult.errorMessage)
                    return@launch
                }
                
                // 画像を圧縮
                val compressedBitmap = compressImage(imageUri, context)
                val compressedBytes = bitmapToByteArray(compressedBitmap)
                
                val ref = storage.reference.child("group_images/$groupId.jpg")
                ref.putBytes(compressedBytes).await()
                val url = ref.downloadUrl.await().toString()
                // Firestoreに保存
                FirebaseFirestore.getInstance().collection("groups").document(groupId)
                    .update("imageUrl", url).await()
                // ローカル状態も更新
                _group.value = _group.value?.copy(imageUrl = url)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "画像のアップロードに失敗しました")
            }
        }
    }
    
    // 画像バリデーション
    private suspend fun validateImage(uri: Uri, context: Context): ValidationResult {
        return kotlinx.coroutines.Dispatchers.IO.run {
            try {
                // 画像のサイズ情報を取得
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                val inputStream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                
                val width = options.outWidth
                val height = options.outHeight
                
                // ファイルサイズを取得
                val fileSize = getFileSize(uri, context)
                
                // バリデーション
                return@run when {
                    width > 4096 || height > 4096 -> {
                        ValidationResult(false, "画像サイズが大きすぎます。4096px以下にしてください。")
                    }
                    fileSize > 10 * 1024 * 1024 -> { // 10MB
                        ValidationResult(false, "ファイルサイズが大きすぎます。10MB以下にしてください。")
                    }
                    width <= 0 || height <= 0 -> {
                        ValidationResult(false, "無効な画像ファイルです。")
                    }
                    else -> {
                        ValidationResult(true, "")
                    }
                }
            } catch (e: Exception) {
                ValidationResult(false, "画像の読み込みに失敗しました。")
            }
        }
    }
    
    // ファイルサイズを取得
    private fun getFileSize(uri: Uri, context: Context): Long {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val size = inputStream?.available()?.toLong() ?: 0
            inputStream?.close()
            size
        } catch (e: Exception) {
            0
        }
    }
    
    // バリデーション結果データクラス
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String
    )
    
    // 画像圧縮処理
    private suspend fun compressImage(uri: Uri, context: Context): Bitmap {
        return kotlinx.coroutines.Dispatchers.IO.run {
            // 元画像を読み込み
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            
            // 圧縮サイズを計算（最大512px）
            val maxSize = 512
            val ratio = minOf(
                maxSize.toFloat() / originalBitmap.width,
                maxSize.toFloat() / originalBitmap.height
            )
            
            val newWidth = (originalBitmap.width * ratio).toInt()
            val newHeight = (originalBitmap.height * ratio).toInt()
            
            // リサイズ
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            
            // 元のBitmapをリサイクル
            originalBitmap.recycle()
            
            resizedBitmap
        }
    }
    
    // BitmapをByteArrayに変換
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        // JPEG圧縮（画質80%）
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        outputStream.close()
        return bytes
    }
    // グループ画像削除
    fun deleteGroupImage(groupId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val ref = storage.reference.child("group_images/$groupId.jpg")
                ref.delete().await()
                FirebaseFirestore.getInstance().collection("groups").document(groupId)
                    .update("imageUrl", "").await()
                _group.value = _group.value?.copy(imageUrl = "")
                onSuccess()
            } catch (e: Exception) {
                // Storageに画像がない場合もFirestoreだけ更新
                FirebaseFirestore.getInstance().collection("groups").document(groupId)
                    .update("imageUrl", "").await()
                _group.value = _group.value?.copy(imageUrl = "")
                onSuccess()
            }
        }
    }
} 