package com.bubblediscipline

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: Mission): Long

    @Query("SELECT * FROM missions_table ORDER BY hour, minute ASC")
    fun getAllMissions(): Flow<List<Mission>>

    @Update
    suspend fun updateMission(mission: Mission)

    @Delete
    suspend fun deleteMission(mission: Mission)
}