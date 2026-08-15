package com.docuvio.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.data.repository.AuthRepository
import com.docuvio.app.data.repository.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import com.docuvio.app.data.model.Organisation

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoadingOrganisations: Boolean = false,
    val organisations: List<Organisation> = emptyList(),
    val selectedOrganisation: Organisation? = null,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val activeSessionToken: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val notificationApi: com.docuvio.app.data.api.NotificationApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        loadOrganisations()
        observeActiveSession()
    }

    private fun observeActiveSession() {
        tokenManager.guestSessionTokenFlow
            .onEach { token ->
                _uiState.update { it.copy(activeSessionToken = token) }
            }
            .launchIn(viewModelScope)
    }

    /* ---------------- LOAD ORGANISATIONS ---------------- */

    fun loadOrganisations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOrganisations = true, error = null) }
            when (val result = authRepository.getOrganisations()) {
                is Result.Success -> {
                    _uiState.update { it.copy(organisations = result.data, isLoadingOrganisations = false) }
                }
                is Result.Error -> {
                    // Handle expired session from backend but don't wipe guest session!
                    if (result.message.contains("401") || result.message.contains("JWT", ignoreCase = true)) {
                        tokenManager.clearAll()
                    }
                    _uiState.update { it.copy(isLoadingOrganisations = false, error = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoadingOrganisations = false, error = "Unexpected error") }
                }
            }
        }
    }

    fun selectOrganisation(organisation: Organisation) {
        _uiState.update { it.copy(selectedOrganisation = organisation) }
    }

    /* ---------------- LOGIN ---------------- */

    fun login(email: String, password: String) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(error = "Email and password cannot be empty") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            when (val result = authRepository.login(email, password)) {
                is Result.Success -> {
                    tokenManager.saveEmail(email)
                    val expiryTime = System.currentTimeMillis() + (60 * 60 * 1000)
                    tokenManager.saveTokenExpiry(expiryTime)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    registerFcmToken()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false, error = "Unexpected error") }
                }
            }
        }
    }

    /* ---------------- SIGNUP ---------------- */

    fun signup(name: String, email: String, password: String, organisationId: String) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(error = "All fields are required") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.signup(name, email, password, organisationId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false, error = "Unexpected error") }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getSavedEmail(): String? = tokenManager.getSavedEmailBlocking()

    private fun registerFcmToken() {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserIdBlocking()
                if (userId.isNullOrBlank()) return@launch
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                notificationApi.registerDevice(
                    com.docuvio.app.data.api.RegisterDeviceRequest(
                        userId = userId, token = fcmToken, platform = "android"
                    )
                )
            } catch (e: Exception) {
                Log.e("FCM", "Failed to register token", e)
            }
        }
    }
}
