package com.example.meintagebuch

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DiaryDao {
    @Insert
    suspend fun insert(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries ORDER BY timestamp DESC")
    fun getAllEntries(): LiveData<List<DiaryEntry>>

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries")
    suspend fun deleteAll()
}