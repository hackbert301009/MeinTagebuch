package com.example.meintagebuch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val filePath: String,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)