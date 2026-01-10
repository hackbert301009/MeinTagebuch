package com.example.meintagebuch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thoughts")
data class ThoughtEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)