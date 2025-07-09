package com.example.tabisuke.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.tabisuke.ui.scheduledetail.Schedule
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

data class ScheduleWithDate(
    val schedule: Schedule,
    val actualDate: String
)

class MainViewModel : ViewModel() {
    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules

    private val _schedulesWithDates = MutableStateFlow<List<ScheduleWithDate>>(emptyList())
    val schedulesWithDates: StateFlow<List<ScheduleWithDate>> = _schedulesWithDates

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
                            dayNumber = (data["dayNumber"] as? Long)?.toInt() ?: 1,
                            time = data["time"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            budget = data["budget"] as? Long ?: 0L,
                            url = data["url"] as? String ?: "",
                            image = data["image"] as? String ?: ""
                        )
                    } else null
                }

                val sortedSchedules = schedulesList.sortedWith(compareBy({ it.dayNumber }, { it.time }))
                
                _schedules.value = sortedSchedules
                
                // 日付付きのスケジュールリストを生成
                val eventData = _event.value
                if (eventData != null && eventData.startDate.isNotEmpty()) {
                    val schedulesWithDates = sortedSchedules.map { schedule ->
                        ScheduleWithDate(
                            schedule = schedule,
                            actualDate = getDateFromDayNumber(schedule.dayNumber, eventData.startDate)
                        )
                    }
                    _schedulesWithDates.value = schedulesWithDates
                }
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }

    // 日数から実際の日付を計算
    private fun getDateFromDayNumber(dayNumber: Int, startDate: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val start = LocalDate.parse(startDate, formatter)
            val targetDate = start.plusDays((dayNumber - 1).toLong())
            targetDate.format(formatter)
        } catch (e: Exception) {
            "日付不明"
        }
    }

    // 日数から表示用の文字列を生成
    fun getDayDisplayText(dayNumber: Int): String {
        return "${dayNumber}日目"
    }
} 