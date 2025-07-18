package com.tabisuke.app.ui.creategroup
import com.tabisuke.app.R

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CreateGroupViewModel : ViewModel() {

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun onGroupNameChange(name: String) {
        _groupName.value = name
    }

    fun createGroup(onSuccess: (groupId: String) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { onFailure(Exception("User not logged in")); return }
        val userName = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Unknown User"

        val newGroupRef = firestore.collection("groups").document()

        // グループデータ構造
        val groupData = hashMapOf(
            "name" to _groupName.value,
            "created_by" to userId,
            "created_at" to com.google.firebase.Timestamp.now()
        )

        // メンバー情報をサブコレクションとして保存
        val memberData = hashMapOf(
            "name" to userName,
            "role" to "owner",
            "joined_at" to com.google.firebase.Timestamp.now()
        )

        firestore.runBatch { batch ->
            // グループデータを保存
            batch.set(newGroupRef, groupData)
            
            // メンバー情報をサブコレクションとして保存
            batch.set(newGroupRef.collection("members").document(userId), memberData)
        }
        .addOnSuccessListener { onSuccess(newGroupRef.id) }
        .addOnFailureListener { e -> onFailure(e) }
    }
}