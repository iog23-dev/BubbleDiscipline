package com.bubblediscipline

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insertHistory(history: MissionHistory): Long

    @Update
    suspend fun updateHistory(history: MissionHistory)

    @Query("UPDATE mission_history SET completionTime = :time, wasPanicUsed = :wasPanic WHERE id = :historyId")
    suspend fun markCompleted(historyId: Int, time: Long, wasPanic: Boolean)

    @Query("SELECT * FROM mission_history ORDER BY activationTime DESC")
    fun getAllHistory(): Flow<List<MissionHistory>>

    @Query("DELETE FROM mission_history")
    suspend fun deleteAllHistory()

    @Query("SELECT * FROM mission_history WHERE activationTime >= :startTime")
    fun getHistorySince(startTime: Long): Flow<List<MissionHistory>>
}