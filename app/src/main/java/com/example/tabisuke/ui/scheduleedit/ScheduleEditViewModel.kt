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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.content.Context
import android.net.Uri
import com.example.tabisuke.utils.ErrorHandler

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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError


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
                _isLoading.value = true
                _errorMessage.value = null
                _hasError.value = false
                
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
                } else {
                    _errorMessage.value = "スケジュールが見つかりません"
                    _hasError.value = true
                }
            } catch (e: Exception) {
                ErrorHandler.logError("ScheduleEditViewModel", "スケジュールの読み込みに失敗", e)
                _errorMessage.value = ErrorHandler.getFirebaseErrorMessage(e)
                _hasError.value = true
            } finally {
                _isLoading.value = false
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

    fun saveSchedule(context: Context, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { onFailure(Exception("User not logged in")); return }

        // 選択された日数を数値で取得（例：「2日目」→ 2）
        val selectedDay = _date.value.replace("日目", "").toIntOrNull() ?: 1

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _hasError.value = false
                
                var imageUrl = _image.value
                
                // 画像が選択されている場合、Firebase Storageにアップロード
                if (_image.value.isNotEmpty() && _image.value.startsWith("content://")) {
                    imageUrl = uploadImageToStorage(_image.value, context)
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
                ErrorHandler.logError("ScheduleEditViewModel", "スケジュールの保存に失敗", e)
                _errorMessage.value = ErrorHandler.getFirebaseErrorMessage(e)
                _hasError.value = true
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
    
    fun updateSchedule(scheduleId: String, context: Context, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { onFailure(Exception("User not logged in")); return }

        // 選択された日数を数値で取得（例：「2日目」→ 2）
        val selectedDay = _date.value.replace("日目", "").toIntOrNull() ?: 1

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _hasError.value = false
                
                var imageUrl = _image.value
                
                // 画像が選択されている場合、Firebase Storageにアップロード
                if (_image.value.isNotEmpty() && _image.value.startsWith("content://")) {
                    imageUrl = uploadImageToStorage(_image.value, context)
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
                ErrorHandler.logError("ScheduleEditViewModel", "スケジュールの更新に失敗", e)
                _errorMessage.value = ErrorHandler.getFirebaseErrorMessage(e)
                _hasError.value = true
                onFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
        _hasError.value = false
    }
    
    private suspend fun uploadImageToStorage(imageUri: String, context: Context): String {
        val storageRef = storage.reference
        val imageRef = storageRef.child("schedule_images/${UUID.randomUUID()}.jpg")
        
        val uri = Uri.parse(imageUri)
        
        // 画像バリデーション
        val validationResult = validateImage(uri, context)
        if (!validationResult.isValid) {
            throw Exception(validationResult.errorMessage)
        }
        
        // 画像を圧縮
        val compressedBitmap = compressImage(uri, context)
        val compressedBytes = bitmapToByteArray(compressedBitmap)
        
        val uploadTask = imageRef.putBytes(compressedBytes)
        val downloadUrl = uploadTask.await().storage.downloadUrl.await()
        return downloadUrl.toString()
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
    
    // バリデーション結果データクラス
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String
    )

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
