package com.bubblediscipline

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Mission::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun missionDao(): MissionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Patrón Singleton para evitar abrir múltiples instancias de la base de datos a la vez
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bubble_discipline_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}