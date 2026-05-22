package com.bubblediscipline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MissionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val missionDao = AppDatabase.getDatabase(application).missionDao()
    
    // Lista de misiones que se actualizará sola en la UI gracias a Flow
    val allMissions: Flow<List<Mission>> = missionDao.getAllMissions()

    // Ejecutamos la inserción dentro del viewModelScope para no bloquear la app
    fun insert(mission: Mission) = viewModelScope.launch {
        missionDao.insertMission(mission)
    }
    
    fun delete(mission: Mission) = viewModelScope.launch {
        missionDao.deleteMission(mission)
    }
}