package com.example.presentmate.data

import kotlinx.coroutines.flow.Flow

import com.google.firebase.auth.FirebaseAuth

class SavedPlacesRepository(private val savedPlaceDao: SavedPlaceDao) {

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "unassigned"

    fun getAll(): Flow<List<SavedPlace>> {
        return savedPlaceDao.getAll(userId)
    }

    suspend fun insert(savedPlace: SavedPlace) {
        val placeToSave = savedPlace.copy(userId = userId)
        savedPlaceDao.insert(placeToSave)
    }

    suspend fun delete(savedPlace: SavedPlace) {
        savedPlaceDao.delete(savedPlace)
    }

    suspend fun getByName(name: String): SavedPlace? {
        return savedPlaceDao.getByName(name, userId)
    }
}
