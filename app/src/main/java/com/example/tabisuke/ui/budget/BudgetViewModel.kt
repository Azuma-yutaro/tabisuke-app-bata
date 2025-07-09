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
    val scheduleCount: Int
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
                            date = data["date"] as? String ?: "",
                            time = data["time"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            budget = data["budget"] as? Long ?: 0L,
                            url = data["url"] as? String ?: "",
                            image = data["image"] as? String ?: ""
                        )
                    } else null
                }
                
                // 日付ごとにグループ化
                val dailyBudgets = schedules.groupBy { it.date }
                    .map { (date, scheduleList) ->
                        DailyBudget(
                            date = date,
                            budget = scheduleList.sumOf { it.budget },
                            scheduleCount = scheduleList.size
                        )
                    }
                    .sortedBy { it.date }
                
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
}

data class Schedule(
    val date: String,
    val time: String,
    val title: String,
    val budget: Long,
    val url: String,
    val image: String
) 