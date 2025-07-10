package com.example.tabisuke.ui.eventlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Event(
    val id: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val createdBy: String,
    val serialEnabled: Boolean,
    val serialCode: String,
    val mapUrl: String
)

class EventListViewModel : ViewModel() {
    
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    fun loadEvents(groupId: String) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val eventsSnapshot = firestore.collection("groups")
                    .document(groupId)
                    .collection("events")
                    .get()
                    .await()
                
                val eventList = mutableListOf<Event>()
                
                for (eventDoc in eventsSnapshot.documents) {
                    val eventId = eventDoc.id
                    val eventData = eventDoc.data
                    
                    if (eventData != null) {
                        val event = Event(
                            id = eventId,
                            title = eventData["title"] as? String ?: "",
                            startDate = eventData["startDate"] as? String ?: eventData["start_date"] as? String ?: "",
                            endDate = eventData["endDate"] as? String ?: eventData["end_date"] as? String ?: "",
                            createdBy = eventData["created_by"] as? String ?: "",
                            serialEnabled = eventData["guestAccessEnabled"] as? Boolean ?: false,
                            serialCode = eventData["guestAccessSerialCode"] as? String ?: "",
                            mapUrl = eventData["map_url"] as? String ?: ""
                        )
                        
                        eventList.add(event)
                    }
                }
                
                _events.value = eventList
            } catch (e: Exception) {
                // エラーハンドリング
            } finally {
                _isLoading.value = false
            }
        }
    }
}
