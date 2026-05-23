package com.bubblediscipline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mission_history")
data class MissionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val missionId: Int,
    val missionMessage: String,
    val activationTime: Long,
    val completionTime: Long? = null,
    val wasPanicUsed: Boolean = false
)