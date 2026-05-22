package com.bubblediscipline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MissionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val missionDao = AppDatabase.getDatabase(application).missionDao()
    private val alarmScheduler = AlarmScheduler(application)
    
    val allMissions: Flow<List<Mission>> = missionDao.getAllMissions()

    fun saveMission(mission: Mission) = viewModelScope.launch {
        val id = missionDao.insertMission(mission)
        // Creamos una copia con el ID real (por si era 0 en una inserción nueva)
        val savedMission = mission.copy(id = id.toInt())
        alarmScheduler.schedule(savedMission)
    }

    fun delete(mission: Mission) = viewModelScope.launch {
        missionDao.deleteMission(mission)
        alarmScheduler.cancel(mission)
    }
}