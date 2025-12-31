package com.example.meintagebuch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey val id: String = "",  // Firebase-ID als Primary Key
    val text: String = "",
    val authorId: String = "ME",
    val timestamp: Long = System.currentTimeMillis()
) {
    // No-Arg Constructor für Firebase
}