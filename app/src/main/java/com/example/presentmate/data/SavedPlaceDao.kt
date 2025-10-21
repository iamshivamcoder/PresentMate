package com.example.presentmate.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savedPlace: SavedPlace)

    @Delete
    suspend fun delete(savedPlace: SavedPlace)

    @Query("SELECT * FROM saved_places ORDER BY name ASC")
    fun getAll(): Flow<List<SavedPlace>>

    @Query("SELECT * FROM saved_places WHERE name = :name")
    suspend fun getByName(name: String): SavedPlace?
}
