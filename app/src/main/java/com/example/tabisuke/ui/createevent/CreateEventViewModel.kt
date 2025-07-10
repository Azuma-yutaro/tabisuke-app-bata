package com.example.tabisuke.ui.createevent

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CreateEventViewModel : ViewModel() {

    private val _eventTitle = MutableStateFlow("")
    val eventTitle: StateFlow<String> = _eventTitle

    private val _startDate = MutableStateFlow("")
    val startDate: StateFlow<String> = _startDate

    private val _endDate = MutableStateFlow("")
    val endDate: StateFlow<String> = _endDate

    private var _groupId = ""

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun setGroupId(groupId: String) {
        _groupId = groupId
    }

    fun onEventTitleChange(title: String) {
        _eventTitle.value = title
    }

    fun onStartDateChange(date: String) {
        _startDate.value = date
    }

    fun onEndDateChange(date: String) {
        _endDate.value = date
    }

    fun createEvent(onSuccess: (eventId: String) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { onFailure(Exception("User not logged in")); return }
        val userName = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Unknown User"

        val newEventRef = firestore.collection("groups").document(_groupId).collection("events").document()

        val eventData = hashMapOf(
            "title" to _eventTitle.value,
            "startDate" to _startDate.value,
            "endDate" to _endDate.value,
            "created_by" to userId,
            "guestAccessEnabled" to false,
            "guestAccessSerialCode" to "",
            "schedules" to emptyList<Map<String, Any>>(),
            "button1" to mapOf("text" to "", "url" to "", "icon" to ""),
            "button2" to mapOf("text" to "", "url" to "", "icon" to ""),
            "button3" to mapOf("text" to "", "url" to "", "icon" to ""),
            "mapUrl" to "",
            "created_at" to com.google.firebase.Timestamp.now()
        )

        newEventRef.set(eventData)
            .addOnSuccessListener { onSuccess(newEventRef.id) }
            .addOnFailureListener { e -> onFailure(e) }
    }
} 