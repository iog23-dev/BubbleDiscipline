package com.bubblediscipline

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Mission::class, MissionHistory::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun missionDao(): MissionDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bubble_discipline_database"
                )
                .fallbackToDestructiveMigration() // Incrementing version to 2
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}