package com.bubblediscipline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Statistics(
    val totalActivated: Int,
    val totalExploded: Int,
    val totalFailed: Int,
    val totalPanic: Int,
    val avgTimeSeconds: Long,
    val weekActivated: Int,
    val monthActivated: Int
)

data class DayStatus(
    val dateLabel: String,
    val success: Boolean?,
    val isToday: Boolean
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val historyDao = AppDatabase.getDatabase(application).historyDao()

    val allHistory: Flow<List<MissionHistory>> = historyDao.getAllHistory()

    val stats: StateFlow<Statistics> = allHistory.map { list ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val startOfWeek = calendar.timeInMillis
        
        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val startOfMonth = calendar.timeInMillis

        val exploded = list.filter { it.completionTime != null && !it.wasPanicUsed }
        val panic = list.count { it.wasPanicUsed }
        
        val failed = list.count { 
            it.completionTime != null && !it.wasPanicUsed && (it.completionTime - it.activationTime > 30 * 60 * 1000) 
        }

        val totalTime = exploded.sumOf { it.completionTime!! - it.activationTime }
        val avgTime = if (exploded.isNotEmpty()) (totalTime / exploded.size) / 1000 else 0L

        Statistics(
            totalActivated = list.size,
            totalExploded = exploded.size,
            totalFailed = failed,
            totalPanic = panic,
            avgTimeSeconds = avgTime,
            weekActivated = list.count { it.activationTime >= startOfWeek },
            monthActivated = list.count { it.activationTime >= startOfMonth }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Statistics(0, 0, 0, 0, 0, 0, 0))

    fun resetStats() {
        viewModelScope.launch {
            historyDao.deleteAllHistory()
        }
    }
    
    fun getDailyResults(daysCount: Int): Flow<List<DayStatus>> {
        return allHistory.map { list ->
            val results = mutableListOf<DayStatus>()
            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            
            for (i in 0 until daysCount) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                val dateLabel = sdf.format(calendar.time)
                val isToday = i == 0
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = calendar.timeInMillis
                
                val daysMissions = list.filter { it.activationTime in startOfDay until endOfDay }
                
                val success = if (daysMissions.isEmpty()) {
                    null // Sin misiones
                } else {
                    val anyPanic = daysMissions.any { it.wasPanicUsed }
                    val allQuick = daysMissions.all { 
                        it.completionTime != null && (it.completionTime - it.activationTime <= 30 * 60 * 1000) 
                    }
                    !anyPanic && allQuick
                }
                results.add(DayStatus(dateLabel, success, isToday))
            }
            results.reversed()
        }
    }
}