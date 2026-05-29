package com.example.presentmate.data

import android.content.Context
import androidx.credentials.CredentialManager
import com.example.presentmate.db.PresentMateDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AuthRepository].
 *
 * Tests the observable properties and non-network methods.
 * Network/sign-in flows require Activity context and are covered by instrumented tests.
 */
class AuthRepositoryTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var database: PresentMateDatabase
    private lateinit var context: Context
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        firebaseAuth = mockk(relaxed = true)
        credentialManager = mockk(relaxed = true)
        database = mockk(relaxed = true)
        context = mockk(relaxed = true)

        repository = AuthRepository(
            firebaseAuth = firebaseAuth,
            credentialManager = credentialManager,
            database = database,
            context = context
        )
    }

    @Test
    fun `currentUser returns non-null when Firebase user is signed in`() {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "user_123"
        }
        every { firebaseAuth.currentUser } returns mockUser

        val result = repository.currentUser

        assertNotNull(result)
        assertEquals("user_123", result?.uid)
    }

    @Test
    fun `currentUser returns null when no user is signed in`() {
        every { firebaseAuth.currentUser } returns null

        val result = repository.currentUser

        assertNull(result)
    }

    @Test
    fun `signOut calls firebaseAuth signOut`() = kotlinx.coroutines.runBlocking {
        repository.signOut()

        verify { firebaseAuth.signOut() }
    }

    @Test
    fun `signOut clears all local database tables`() = kotlinx.coroutines.runBlocking {
        repository.signOut()

        coVerify { database.clearAllTables() }
    }
}
