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

data class PeriodStats(
    val activated: Int,
    val exploded: Int,
    val failed: Int,
    val panic: Int
)

data class Statistics(
    val day: PeriodStats,
    val week: PeriodStats,
    val month: PeriodStats,
    val total: PeriodStats,
    val avgTimeSeconds: Long
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
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis
        
        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        fun calculatePeriodStats(items: List<MissionHistory>): PeriodStats {
            val exploded = items.count { it.completionTime != null && !it.wasPanicUsed }
            val panic = items.count { it.wasPanicUsed }
            val failed = items.count { 
                it.completionTime != null && !it.wasPanicUsed && (it.completionTime - it.activationTime > 30 * 60 * 1000) 
            }
            return PeriodStats(items.size, exploded, failed, panic)
        }

        val explodedList = list.filter { it.completionTime != null && !it.wasPanicUsed }
        val totalTime = explodedList.sumOf { it.completionTime!! - it.activationTime }
        val avgTime = if (explodedList.isNotEmpty()) (totalTime / explodedList.size) / 1000 else 0L

        Statistics(
            day = calculatePeriodStats(list.filter { it.activationTime >= startOfDay }),
            week = calculatePeriodStats(list.filter { it.activationTime >= startOfWeek }),
            month = calculatePeriodStats(list.filter { it.activationTime >= startOfMonth }),
            total = calculatePeriodStats(list),
            avgTimeSeconds = avgTime
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Statistics(
        PeriodStats(0, 0, 0, 0),
        PeriodStats(0, 0, 0, 0),
        PeriodStats(0, 0, 0, 0),
        PeriodStats(0, 0, 0, 0),
        0
    ))

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