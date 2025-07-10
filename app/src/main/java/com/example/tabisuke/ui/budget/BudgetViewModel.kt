package com.example.tabisuke.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DailyBudget(
    val date: String,
    val budget: Long,
    val scheduleCount: Int,
    val dayLabel: String
)

data class ScheduleDetail(
    val title: String,
    val budget: Long
)

data class BudgetSummary(
    val totalBudget: Long,
    val dailyBudgets: List<DailyBudget>
)

class BudgetViewModel : ViewModel() {
    
    private val _budgetSummary = MutableStateFlow(BudgetSummary(0L, emptyList()))
    val budgetSummary: StateFlow<BudgetSummary> = _budgetSummary
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _scheduleDetails = MutableStateFlow<List<ScheduleDetail>>(emptyList())
    val scheduleDetails: StateFlow<List<ScheduleDetail>> = _scheduleDetails
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    fun loadBudgetData(groupId: String, eventId: String) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val schedulesSnapshot = firestore.collection("groups")
                    .document(groupId)
                    .collection("events")
                    .document(eventId)
                    .collection("schedules")
                    .get()
                    .await()
                
                val schedules = schedulesSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) {
                        Schedule(
                            date = data["dayNumber"]?.toString() ?: "",
                            time = data["time"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            budget = data["budget"] as? Long ?: 0L,
                            url = data["url"] as? String ?: "",
                            image = data["image"] as? String ?: ""
                        )
                    } else null
                }
                
                // 日付ごとにグループ化（数値として並び替え）
                val sortedDates = schedules.map { it.date.toIntOrNull() ?: 0 }.distinct().sorted()
                val dailyBudgets = sortedDates.mapNotNull { dayNumber ->
                    val scheduleList = schedules.filter { it.date == dayNumber.toString() }
                    val totalBudget = scheduleList.sumOf { it.budget }
                    
                    // 予算が0円の場合は表示しない
                    if (totalBudget > 0) {
                        DailyBudget(
                            date = dayNumber.toString(),
                            budget = totalBudget,
                            scheduleCount = scheduleList.size,
                            dayLabel = "${dayNumber}日目"
                        )
                    } else {
                        null
                    }
                }
                
                val totalBudget = schedules.sumOf { it.budget }
                
                _budgetSummary.value = BudgetSummary(
                    totalBudget = totalBudget,
                    dailyBudgets = dailyBudgets
                )
            } catch (e: Exception) {
                // エラーハンドリング
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadScheduleDetails(groupId: String, eventId: String, dayNumber: String) {
        viewModelScope.launch {
            try {
                val schedulesSnapshot = firestore.collection("groups")
                    .document(groupId)
                    .collection("events")
                    .document(eventId)
                    .collection("schedules")
                    .whereEqualTo("dayNumber", dayNumber.toIntOrNull() ?: 0)
                    .get()
                    .await()
                
                val details = schedulesSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) {
                        ScheduleDetail(
                            title = data["title"] as? String ?: "",
                            budget = data["budget"] as? Long ?: 0L
                        )
                    } else null
                }.filter { it.budget > 0 } // 予算が0円のものは除外
                
                _scheduleDetails.value = details
            } catch (e: Exception) {
                _scheduleDetails.value = emptyList()
            }
        }
    }
}

data class Schedule(
    val date: String,
    val time: String,
    val title: String,
    val budget: Long,
    val url: String,
    val image: String
) 