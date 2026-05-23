package com.bubblediscipline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missions_table")
data class Mission(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String,      // Ej: "¡Haz la cama!"
    val hour: Int,            // Hora en formato 24h (0-23)
    val minute: Int,          // Minuto (0-59)
    val isEnabled: Boolean = true, // Si la alarma está activa o apagada
    val category: String = "General", // Categoría de la tarea
    
    // Días de la semana
    val monday: Boolean = false,
    val tuesday: Boolean = false,
    val wednesday: Boolean = false,
    val thursday: Boolean = false,
    val friday: Boolean = false,
    val saturday: Boolean = false,
    val sunday: Boolean = false
)