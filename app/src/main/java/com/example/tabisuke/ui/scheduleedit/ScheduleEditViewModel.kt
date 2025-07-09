package com.example.tabisuke.ui.scheduleedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading



    private var _groupId = ""
    private var _eventId = ""
    private var _scheduleId = ""

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    fun setGroupAndEventId(groupId: String, eventId: String) {
        _groupId = groupId
        _eventId = eventId
        loadEventPeriod()
    }
    

    
    fun loadSchedule(scheduleId: String) {
        _scheduleId = scheduleId
        viewModelScope.launch {
            try {
                val scheduleDoc = firestore.collection("groups")
                    .document(_groupId)
                    .collection("events")
                    .document(_eventId)
                    .collection("schedules")
                    .document(scheduleId)
                    .get()
                    .await()

                if (scheduleDoc.exists()) {
                    val data = scheduleDoc.data!!
                    val dayNumber = data["dayNumber"] as? Long ?: 1
                    _date.value = "${dayNumber}日目"
                    _time.value = data["time"] as? String ?: ""
                    _title.value = data["title"] as? String ?: ""
                    _budget.value = (data["budget"] as? Long ?: 0L).toString()
                    _url.value = data["url"] as? String ?: ""
                    _image.value = data["image"] as? String ?: ""
                }
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
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
    
    fun onImageUriChange(imageUri: String) {
        _image.value = imageUri
    }

    fun saveSchedule(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { onFailure(Exception("User not logged in")); return }

        // 選択された日数を数値で取得（例：「2日目」→ 2）
        val selectedDay = _date.value.replace("日目", "").toIntOrNull() ?: 1

        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                var imageUrl = _image.value
                
                // 画像が選択されている場合、Firebase Storageにアップロード
                if (_image.value.isNotEmpty() && _image.value.startsWith("content://")) {
                    imageUrl = uploadImageToStorage(_image.value)
                }

                val scheduleData = hashMapOf(
                    "dayNumber" to selectedDay, // 何日目かを数値で保存
                    "time" to _time.value,
                    "title" to _title.value,
                    "budget" to (_budget.value.toLongOrNull() ?: 0L),
                    "url" to _url.value,
                    "image" to imageUrl,
                    "created_by" to userId,
                    "created_at" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("groups")
                    .document(_groupId)
                    .collection("events")
                    .document(_eventId)
                    .collection("schedules")
                    .add(scheduleData)
                    .await()
                
                // 保存成功後、入力欄をクリア（日数と時間は保持）
                clearInputFields()
                
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // 入力欄をクリアする関数（日数と時間は保持）
    private fun clearInputFields() {
        _title.value = ""
        _budget.value = ""
        _url.value = ""
        _image.value = ""
    }
    
    fun updateSchedule(scheduleId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { onFailure(Exception("User not logged in")); return }

        // 選択された日数を数値で取得（例：「2日目」→ 2）
        val selectedDay = _date.value.replace("日目", "").toIntOrNull() ?: 1

        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                var imageUrl = _image.value
                
                // 画像が選択されている場合、Firebase Storageにアップロード
                if (_image.value.isNotEmpty() && _image.value.startsWith("content://")) {
                    imageUrl = uploadImageToStorage(_image.value)
                }

                val scheduleData = mapOf(
                    "dayNumber" to selectedDay, // 何日目かを数値で保存
                    "time" to _time.value,
                    "title" to _title.value,
                    "budget" to (_budget.value.toLongOrNull() ?: 0L),
                    "url" to _url.value,
                    "image" to imageUrl,
                    "updated_by" to userId,
                    "updated_at" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("groups")
                    .document(_groupId)
                    .collection("events")
                    .document(_eventId)
                    .collection("schedules")
                    .document(scheduleId)
                    .update(scheduleData)
                    .await()
                
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun uploadImageToStorage(imageUri: String): String {
        val storageRef = storage.reference
        val imageRef = storageRef.child("schedule_images/${UUID.randomUUID()}.jpg")
        
        val uri = android.net.Uri.parse(imageUri)
        val uploadTask = imageRef.putFile(uri)
        
        val downloadUrl = uploadTask.await().storage.downloadUrl.await()
        return downloadUrl.toString()
    }
    
    // 行事を削除する関数
    fun deleteSchedule(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { onFailure(Exception("User not logged in")); return }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 画像が存在する場合、Firebase Storageから削除
                if (_image.value.isNotEmpty() && !_image.value.startsWith("content://")) {
                    deleteImageFromStorage(_image.value)
                }
                
                // Firestoreからスケジュールを削除
                firestore.collection("groups")
                    .document(_groupId)
                    .collection("events")
                    .document(_eventId)
                    .collection("schedules")
                    .document(_scheduleId)
                    .delete()
                    .await()
                
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Firebase Storageから画像を削除
    private suspend fun deleteImageFromStorage(imageUrl: String) {
        try {
            val storageRef = storage.reference
            val imageRef = storageRef.storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
        } catch (e: Exception) {
            // 画像削除に失敗しても処理を続行
        }
    }
}
