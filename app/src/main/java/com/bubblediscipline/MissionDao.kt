package com.bubblediscipline

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    // Insertar una nueva misión. Si ya existe el ID, lo reemplaza.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: Mission)

    // Obtener todas las misiones ordenadas por hora. 
    // Usamos Flow para que la interfaz de Compose se actualice sola en tiempo real cuando haya cambios.
    @Query("SELECT * FROM missions_table ORDER BY hour, minute ASC")
    fun getAllMissions(): Flow<List<Mission>>

    @Update
    suspend fun updateMission(mission: Mission)

    @Delete
    suspend fun deleteMission(mission: Mission)
}