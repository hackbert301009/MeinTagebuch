package com.example.meintagebuch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String = "",
    val authorId: String = "ME",
    val timestamp: Long = System.currentTimeMillis()
) {
    // No-Arg Constructor für Firebase (wird automatisch generiert durch data class mit Defaults)
}