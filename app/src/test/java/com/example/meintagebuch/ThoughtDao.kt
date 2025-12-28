package com.example.meintagebuch

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ThoughtDao {
    @Insert
    suspend fun insert(thought: ThoughtEntry)

    @Query("SELECT * FROM thoughts ORDER BY timestamp DESC")
    fun getAllThoughts(): LiveData<List<ThoughtEntry>>

    @Query("SELECT COUNT(*) FROM thoughts WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getThoughtsCountForDay(startOfDay: Long, endOfDay: Long): LiveData<Int>

    @Query("SELECT * FROM thoughts WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getThoughtsForDay(startOfDay: Long, endOfDay: Long): LiveData<List<ThoughtEntry>>

    @Query("DELETE FROM thoughts")
    suspend fun deleteAll()
}