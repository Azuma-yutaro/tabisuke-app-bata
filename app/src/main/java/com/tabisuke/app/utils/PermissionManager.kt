package com.tabisuke.app.utils
import com.tabisuke.app.R

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class UserRole {
    OWNER, EDITOR, VIEWER, GUEST
}

data class UserPermission(
    val userId: String,
    val name: String,
    val role: UserRole
)

class PermissionManager {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * ユーザーがグループのメンバーかどうかをチェック
     */
    suspend fun isGroupMember(groupId: String, userId: String? = null): Boolean {
        val currentUserId = userId ?: auth.currentUser?.uid ?: return false
        
        return try {
            val memberDoc = firestore.collection("groups")
                .document(groupId)
                .collection("members")
                .document(currentUserId)
                .get()
                .await()
            
            memberDoc.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * ユーザーの権限を取得
     */
    suspend fun getUserRole(groupId: String, userId: String? = null): UserRole {
        val currentUserId = userId ?: auth.currentUser?.uid ?: return UserRole.GUEST
        
        return try {
            val memberDoc = firestore.collection("groups")
                .document(groupId)
                .collection("members")
                .document(currentUserId)
                .get()
                .await()
            
            if (!memberDoc.exists()) return UserRole.GUEST
            
            val role = memberDoc.getString("role") ?: "viewer"
            when (role) {
                "owner" -> UserRole.OWNER
                "editor" -> UserRole.EDITOR
                "viewer" -> UserRole.VIEWER
                else -> UserRole.VIEWER
            }
        } catch (e: Exception) {
            UserRole.GUEST
        }
    }

    /**
     * イベント作成者かどうかをチェック
     */
    suspend fun isEventCreator(groupId: String, eventId: String, userId: String? = null): Boolean {
        val currentUserId = userId ?: auth.currentUser?.uid ?: return false
        
        return try {
            val eventDoc = firestore.collection("groups")
                .document(groupId)
                .collection("events")
                .document(eventId)
                .get()
                .await()
            
            if (!eventDoc.exists()) return false
            
            val createdBy = eventDoc.getString("created_by")
            createdBy == currentUserId
        } catch (e: Exception) {
            false
        }
    }

    /**
     * シリアルコードでゲストアクセスをチェック
     */
    suspend fun checkSerialCodeAccess(groupId: String, eventId: String, serialCode: String): Boolean {
        return try {
            val eventDoc = firestore.collection("groups")
                .document(groupId)
                .collection("events")
                .document(eventId)
                .get()
                .await()
            
            if (!eventDoc.exists()) return false
            
            val serialEnabled = eventDoc.getBoolean("guestAccessEnabled") ?: false
            val storedSerialCode = eventDoc.getString("guestAccessSerialCode") ?: ""
            
            serialEnabled && serialCode == storedSerialCode
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 編集権限があるかチェック
     */
    suspend fun canEdit(groupId: String, eventId: String, userId: String? = null): Boolean {
        val role = getUserRole(groupId, userId)
        val isCreator = isEventCreator(groupId, eventId, userId)
        
        return role == UserRole.OWNER || role == UserRole.EDITOR || isCreator
    }

    /**
     * 削除権限があるかチェック
     */
    suspend fun canDelete(groupId: String, eventId: String, userId: String? = null): Boolean {
        val role = getUserRole(groupId, userId)
        val isCreator = isEventCreator(groupId, eventId, userId)
        
        return role == UserRole.OWNER || isCreator
    }

    /**
     * 閲覧権限があるかチェック
     */
    suspend fun canView(groupId: String, eventId: String, userId: String? = null): Boolean {
        val isMember = isGroupMember(groupId, userId)
        return isMember
    }
} 