package com.example.meintagebuch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ownerId: String = "me"

)