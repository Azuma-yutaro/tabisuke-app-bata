package com.example.tabisuke.ui.scheduleedit

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ScheduleEditViewModel : ViewModel() {

    private val _date = MutableStateFlow("")
    val date: StateFlow<String> = _date

    private val _time = MutableStateFlow("")
    val time: StateFlow<String> = _time

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _budget = MutableStateFlow("")
    val budget: StateFlow<String> = _budget

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url

    private val _image = MutableStateFlow("")
    val image: StateFlow<String> = _image

    private var _groupId = ""
    private var _eventId = ""

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun setGroupAndEventId(groupId: String, eventId: String) {
        _groupId = groupId
        _eventId = eventId
    }

    fun onDateChange(date: String) {
        _date.value = date
    }

    fun onTimeChange(time: String) {
        _time.value = time
    }

    fun onTitleChange(title: String) {
        _title.value = title
    }

    fun onBudgetChange(budget: String) {
        _budget.value = budget
    }

    fun onUrlChange(url: String) {
        _url.value = url
    }

    fun onImageChange(image: String) {
        _image.value = image
    }

    fun saveSchedule(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { onFailure(Exception("User not logged in")); return }

        val scheduleData = hashMapOf(
            "date" to _date.value,
            "time" to _time.value,
            "title" to _title.value,
            "budget" to (_budget.value.toLongOrNull() ?: 0L),
            "url" to _url.value,
            "image" to _image.value,
            "created_by" to userId,
            "created_at" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("groups")
            .document(_groupId)
            .collection("events")
            .document(_eventId)
            .collection("schedules")
            .add(scheduleData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
