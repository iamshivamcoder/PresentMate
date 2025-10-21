package com.example.presentmate.data

import kotlinx.coroutines.flow.Flow

class SavedPlacesRepository(private val savedPlaceDao: SavedPlaceDao) {

    fun getAll(): Flow<List<SavedPlace>> {
        return savedPlaceDao.getAll()
    }

    suspend fun insert(savedPlace: SavedPlace) {
        savedPlaceDao.insert(savedPlace)
    }

    suspend fun delete(savedPlace: SavedPlace) {
        savedPlaceDao.delete(savedPlace)
    }

    suspend fun getByName(name: String): SavedPlace? {
        return savedPlaceDao.getByName(name)
    }
}
