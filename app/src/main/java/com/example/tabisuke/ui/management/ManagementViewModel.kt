package com.example.tabisuke.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

data class Member(
    val personalId: String,
    val name: String,
    val role: String
)

data class GuestAccess(
    val enabled: Boolean,
    val serialCode: String
)

class ManagementViewModel : ViewModel() {
    
    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event
    
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members
    
    private val _guestAccess = MutableStateFlow(GuestAccess(false, ""))
    val guestAccess: StateFlow<GuestAccess> = _guestAccess
    
    // 日付選択関連
    private val _showStartDatePicker = MutableStateFlow(false)
    val showStartDatePicker: StateFlow<Boolean> = _showStartDatePicker
    
    private val _showEndDatePicker = MutableStateFlow(false)
    val showEndDatePicker: StateFlow<Boolean> = _showEndDatePicker
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    fun loadEvent(groupId: String, eventId: String) {
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
                        startDate = data["startDate"] as? String ?: data["start_date"] as? String ?: "",
                        endDate = data["endDate"] as? String ?: data["end_date"] as? String ?: "",
                        mapUrl = data["mapUrl"] as? String ?: data["map_url"] as? String ?: "",
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
                
                // メンバー情報を読み込み
                loadMembers(groupId)
                
                // ゲストアクセス設定を読み込み
                loadGuestAccess(groupId, eventId)
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }

    fun loadMembers(groupId: String) {
        viewModelScope.launch {
            try {
                val groupDoc = firestore.collection("groups")
                    .document(groupId)
                    .get()
                    .await()

                if (groupDoc.exists()) {
                    val data = groupDoc.data!!
                    val membersMap = data["members"] as? Map<String, Map<String, String>>
                    val memberList = membersMap?.map { (personalId, memberData) ->
                        Member(
                            personalId = personalId,
                            name = memberData["name"] ?: "",
                            role = memberData["role"] ?: "viewer"
                        )
                    } ?: emptyList()
                    _members.value = memberList
                }
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }

    fun loadGuestAccess(groupId: String, eventId: String) {
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
                    _guestAccess.value = GuestAccess(
                        enabled = data["guestAccessEnabled"] as? Boolean ?: false,
                        serialCode = data["guestAccessSerialCode"] as? String ?: ""
                    )
                }
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }

    // オーナー権限チェック
    fun isOwner(userId: String): Boolean {
        return _members.value.any { member ->
            member.personalId == userId && member.role == "owner"
        }
    }

    // イベント削除
    fun deleteEvent(groupId: String, eventId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                // イベントを削除
                firestore.collection("groups")
                    .document(groupId)
                    .collection("events")
                    .document(eventId)
                    .delete()
                    .await()
                
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    // 日付選択関連
    fun showStartDatePicker() {
        _showStartDatePicker.value = true
    }

    fun hideStartDatePicker() {
        _showStartDatePicker.value = false
    }

    fun showEndDatePicker() {
        _showEndDatePicker.value = true
    }

    fun hideEndDatePicker() {
        _showEndDatePicker.value = false
    }

    fun updateEventTitle(title: String) {
        _event.value = _event.value?.copy(title = title)
    }

    fun updateEventDescription(description: String) {
        _event.value = _event.value?.copy(description = description)
    }

    fun updateEventStartDate(startDate: String) {
        _event.value = _event.value?.copy(startDate = startDate)
    }

    fun updateEventEndDate(endDate: String) {
        _event.value = _event.value?.copy(endDate = endDate)
    }

    fun updateEventMapUrl(mapUrl: String) {
        _event.value = _event.value?.copy(mapUrl = mapUrl)
    }

    fun updateButton1Text(text: String) {
        _event.value = _event.value?.copy(
            button1 = _event.value?.button1?.copy(text = text) ?: ButtonConfig("", "", "")
        )
    }

    fun updateButton1Url(url: String) {
        _event.value = _event.value?.copy(
            button1 = _event.value?.button1?.copy(url = url) ?: ButtonConfig("", "", "")
        )
    }

    fun updateButton1Icon(icon: String) {
        _event.value = _event.value?.copy(
            button1 = _event.value?.button1?.copy(icon = icon) ?: ButtonConfig("", "", "")
        )
    }

    fun updateButton2Text(text: String) {
        _event.value = _event.value?.copy(
            button2 = _event.value?.button2?.copy(text = text) ?: ButtonConfig("", "", "")
        )
    }

    fun updateButton2Url(url: String) {
        _event.value = _event.value?.copy(
            button2 = _event.value?.button2?.copy(url = url) ?: ButtonConfig("", "", "")
        )
    }

    fun updateButton2Icon(icon: String) {
        _event.value = _event.value?.copy(
            button2 = _event.value?.button2?.copy(icon = icon) ?: ButtonConfig("", "", "")
        )
    }

    fun updateButton3Text(text: String) {
        _event.value = _event.value?.copy(
            button3 = _event.value?.button3?.copy(text = text) ?: ButtonConfig("", "", "")
        )
    }

    fun updateButton3Url(url: String) {
        _event.value = _event.value?.copy(
            button3 = _event.value?.button3?.copy(url = url) ?: ButtonConfig("", "", "")
        )
    }

    fun updateButton3Icon(icon: String) {
        _event.value = _event.value?.copy(
            button3 = _event.value?.button3?.copy(icon = icon) ?: ButtonConfig("", "", "")
        )
    }

    fun updateMemberRole(groupId: String, personalId: String, newRole: String) {
        viewModelScope.launch {
            try {
                firestore.collection("groups")
                    .document(groupId)
                    .update("members.$personalId.role", newRole)
                    .await()
                
                // ローカル状態も更新
                _members.value = _members.value.map { member ->
                    if (member.personalId == personalId) {
                        member.copy(role = newRole)
                    } else {
                        member
                    }
                }
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }

    fun updateGuestAccessEnabled(enabled: Boolean) {
        _guestAccess.value = _guestAccess.value.copy(enabled = enabled)
    }

    fun updateGuestAccessSerialCode(serialCode: String) {
        _guestAccess.value = _guestAccess.value.copy(serialCode = serialCode)
    }

    fun saveEvent(groupId: String, eventId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val event = _event.value ?: run { onFailure(Exception("Event data is null")); return }
        val guestAccessData = _guestAccess.value
        
        val eventData = hashMapOf(
            "title" to event.title,
            "description" to event.description,
            "startDate" to event.startDate,
            "endDate" to event.endDate,
            "mapUrl" to event.mapUrl,
            "button1Text" to event.button1.text,
            "button1Icon" to event.button1.icon,
            "button1Url" to event.button1.url,
            "button2Text" to event.button2.text,
            "button2Icon" to event.button2.icon,
            "button2Url" to event.button2.url,
            "button3Text" to event.button3.text,
            "button3Icon" to event.button3.icon,
            "button3Url" to event.button3.url,
            "guestAccessEnabled" to guestAccessData.enabled,
            "guestAccessSerialCode" to guestAccessData.serialCode,
            "updated_at" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("groups")
            .document(groupId)
            .collection("events")
            .document(eventId)
            .update(eventData as Map<String, Any>)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
