package com.example.tabisuke.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.tabisuke.ui.scheduledetail.Schedule

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val mapUrl: String,
    val button1: ButtonConfig,
    val button2: ButtonConfig,
    val button3: ButtonConfig
)

data class ButtonConfig(
    val text: String,
    val icon: String,
    val url: String
)

class MainViewModel : ViewModel() {
    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun fetchEvent(groupId: String, eventId: String) {
        viewModelScope.launch {
            try {
                val eventDoc = firestore.collection("groups")
                    .document(groupId)
                    .collection("events")
                    .document(eventId)
                    .get()
                    .await()

                if (eventDoc.exists()) {
                    val data = eventDoc.data!!
                    _event.value = Event(
                        id = eventId,
                        title = data["title"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        startDate = data["startDate"] as? String ?: "",
                        endDate = data["endDate"] as? String ?: "",
                        mapUrl = data["mapUrl"] as? String ?: "",
                        button1 = ButtonConfig(
                            text = data["button1Text"] as? String ?: "",
                            icon = data["button1Icon"] as? String ?: "",
                            url = data["button1Url"] as? String ?: ""
                        ),
                        button2 = ButtonConfig(
                            text = data["button2Text"] as? String ?: "",
                            icon = data["button2Icon"] as? String ?: "",
                            url = data["button2Url"] as? String ?: ""
                        ),
                        button3 = ButtonConfig(
                            text = data["button3Text"] as? String ?: "",
                            icon = data["button3Icon"] as? String ?: "",
                            url = data["button3Url"] as? String ?: ""
                        )
                    )
                }
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }

    fun loadSchedules(groupId: String, eventId: String) {
        viewModelScope.launch {
            try {
                val schedulesSnapshot = firestore.collection("groups")
                    .document(groupId)
                    .collection("events")
                    .document(eventId)
                    .collection("schedules")
                    .get()
                    .await()

                val schedulesList = schedulesSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) {
                        Schedule(
                            date = data["date"] as? String ?: "",
                            time = data["time"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            budget = data["budget"] as? Long ?: 0L,
                            url = data["url"] as? String ?: "",
                            image = data["image"] as? String ?: ""
                        )
                    } else null
                }

                _schedules.value = schedulesList.sortedWith(compareBy({ it.date }, { it.time }))
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }
}
