package com.example.presentmate.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.example.presentmate.db.PresentMateDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedPlaceDaoTest {

    private lateinit var database: PresentMateDatabase
    private lateinit var dao: SavedPlaceDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            PresentMateDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.savedPlaceDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetByName() = runBlocking {
        val place = SavedPlace(
            name = "Home",
            address = "123 Home St",
            latitude = 12.34,
            longitude = 56.78,
            userId = "test_user"
        )

        dao.insert(place)

        val fetchedPlace = dao.getByName("Home", "test_user")
        assertNotNull(fetchedPlace)
        assertEquals(12.34, fetchedPlace!!.latitude, 0.001)
        assertEquals("test_user", fetchedPlace.userId)
    }

    @Test
    fun deletePlace() = runBlocking {
        val place = SavedPlace(
            name = "Work",
            address = "456 Work Ave",
            latitude = 12.34,
            longitude = 56.78,
            userId = "test_user"
        )

        dao.insert(place)
        val fetchedPlace = dao.getByName("Work", "test_user")
        assertNotNull(fetchedPlace)

        dao.delete(fetchedPlace!!)

        val deletedPlace = dao.getByName("Work", "test_user")
        assertNull(deletedPlace)
    }

    @Test
    fun getAllPlacesOrderedByName() = runBlocking {
        dao.insert(SavedPlace(name = "Zeta",  address = "A", latitude = 0.0, longitude = 0.0, userId = "test_user"))
        dao.insert(SavedPlace(name = "Alpha", address = "A", latitude = 0.0, longitude = 0.0, userId = "test_user"))
        dao.insert(SavedPlace(name = "Gamma", address = "A", latitude = 0.0, longitude = 0.0, userId = "test_user"))

        val places = dao.getAll("test_user").first()

        assertEquals(3, places.size)
        assertEquals("Alpha", places[0].name)
        assertEquals("Gamma", places[1].name)
        assertEquals("Zeta", places[2].name)
    }

    @Test
    fun getAllPlaces_cross_user_isolation() = runBlocking {
        // Insert one place for user_a
        dao.insert(SavedPlace(name = "Secret HQ", address = "A", latitude = 0.0, longitude = 0.0, userId = "user_a"))

        // user_b should see nothing
        val placesForB = dao.getAll("user_b").first()
        assertEquals(0, placesForB.size)

        // user_a sees their own
        val placesForA = dao.getAll("user_a").first()
        assertEquals(1, placesForA.size)
        assertEquals("Secret HQ", placesForA[0].name)
    }
}
