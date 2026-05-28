package com.example.presentmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.presentmate.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val driveSyncManager: com.example.presentmate.data.DriveSyncManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(
        if (authRepository.currentUser != null) AuthState.Authenticated else AuthState.Idle
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signInWithGoogle(context: android.content.Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(context)
            handleAuthResult(result)
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(email, password)
            handleAuthResult(result)
        }
    }

    fun signInWithEmail(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, password)
            handleAuthResult(result)
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }

    private suspend fun handleAuthResult(result: Result<*>) {
        if (result.isSuccess) {
            _authState.value = AuthState.Loading // Keep loading while syncing
            val syncResult = driveSyncManager.restoreDatabaseFromDrive()
            
            if (syncResult.isFailure && syncResult.exceptionOrNull() is com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                // We'll just ignore for now if the user hasn't granted permissions on first login
                // The app will prompt for permission when they try to backup manually from Settings
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Authenticated
            }
        } else {
            _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Unknown error occurred")
        }
    }

    fun resetState() {
        if (authRepository.currentUser != null) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Idle
        }
    }

    fun backupDatabaseToDrive(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = driveSyncManager.backupDatabaseToDrive()
            onResult(result)
        }
    }

    fun restoreDatabaseFromDrive(onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val result = driveSyncManager.restoreDatabaseFromDrive()
            onResult(result)
        }
    }
}
