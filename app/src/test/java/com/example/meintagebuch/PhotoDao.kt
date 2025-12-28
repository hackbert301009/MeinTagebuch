package com.example.meintagebuch

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: PhotoEntry)

    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): LiveData<List<PhotoEntry>>

    @Delete
    suspend fun delete(photo: PhotoEntry)

    @Query("DELETE FROM photos")
    suspend fun deleteAll()

    @Update
    suspend fun update(photo: PhotoEntry)
}