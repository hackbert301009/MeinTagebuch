package com.example.meintagebuch

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ThoughtEntry::class, DiaryEntry::class, PhotoEntry::class, PartnerInvite::class],
    version = 103,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun thoughtDao(): ThoughtDao
    abstract fun diaryDao(): DiaryDao
    abstract fun photoDao(): PhotoDao
    abstract fun partnerInviteDao(): PartnerInviteDao   // <-- Korrigiert

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diary_database"
                ).fallbackToDestructiveMigration() // optional beim Testen
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
