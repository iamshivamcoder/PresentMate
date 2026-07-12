package com.example.presentmate.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeveloperIdeaDao {
    @Query("SELECT * FROM developer_ideas WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllIdeas(userId: String): Flow<List<DeveloperIdea>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: DeveloperIdea)

    @Update
    suspend fun updateIdea(idea: DeveloperIdea)

    @Delete
    suspend fun deleteIdea(idea: DeveloperIdea)
}
