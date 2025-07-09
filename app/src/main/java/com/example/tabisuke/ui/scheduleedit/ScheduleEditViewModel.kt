package com.example.tabisuke.ui.scheduleedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    // イベント期間関連
    private val _eventStartDate = MutableStateFlow<LocalDate?>(null)
    val eventStartDate: StateFlow<LocalDate?> = _eventStartDate

    private val _eventEndDate = MutableStateFlow<LocalDate?>(null)
    val eventEndDate: StateFlow<LocalDate?> = _eventEndDate

    private val _dayOptions = MutableStateFlow<List<String>>(emptyList())
    val dayOptions: StateFlow<List<String>> = _dayOptions

    private var _groupId = ""
    private var _eventId = ""

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun setGroupAndEventId(groupId: String, eventId: String) {
        _groupId = groupId
        _eventId = eventId
        loadEventPeriod()
    }

    // イベント期間を読み込み
    private fun loadEventPeriod() {
        viewModelScope.launch {
            try {
                val eventDoc = firestore.collection("groups")
                    .document(_groupId)
                    .collection("events")
                    .document(_eventId)
                    .get()
                    .await()

                if (eventDoc.exists()) {
                    val data = eventDoc.data!!
                    val startDateStr = data["startDate"] as? String ?: ""
                    val endDateStr = data["endDate"] as? String ?: ""

                    if (startDateStr.isNotEmpty() && endDateStr.isNotEmpty()) {
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val startDate = LocalDate.parse(startDateStr, formatter)
                        val endDate = LocalDate.parse(endDateStr, formatter)
                        
                        _eventStartDate.value = startDate
                        _eventEndDate.value = endDate
                        
                        // 日数オプションを生成
                        generateDayOptions(startDate, endDate)
                    }
                }
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }

    // 日数オプションを生成
    private fun generateDayOptions(startDate: LocalDate, endDate: LocalDate) {
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
        val options = (1..daysBetween.toInt()).map { day ->
            "${day}日目"
        }
        _dayOptions.value = options
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

        // 選択された日数を数値で取得（例：「2日目」→ 2）
        val selectedDay = _date.value.replace("日目", "").toIntOrNull() ?: 1

        val scheduleData = hashMapOf(
            "dayNumber" to selectedDay, // 何日目かを数値で保存
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
