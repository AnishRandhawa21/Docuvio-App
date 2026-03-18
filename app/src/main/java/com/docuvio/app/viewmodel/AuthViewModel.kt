package com.docuvio.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.data.repository.AuthRepository
import com.docuvio.app.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isSuccess: Boolean = false
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
    }

    /* ---------------- LOAD ORGANISATIONS ---------------- */

    fun loadOrganisations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingOrganisations = true,
                error = null
            )

            when (val result = authRepository.getOrganisations()) {

                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        organisations = result.data,
                        isLoadingOrganisations = false
                    )
                }

                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingOrganisations = false,
                        error = result.message
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingOrganisations = false,
                        error = "Unexpected error"
                    )
                }
            }
        }
    }

    fun selectOrganisation(organisation: Organisation) {
        _uiState.value = _uiState.value.copy(
            selectedOrganisation = organisation
        )
    }

    /* ---------------- LOGIN ---------------- */

    fun login(email: String, password: String) {
        viewModelScope.launch {

            if (email.isBlank() || password.isBlank()) {
                _uiState.value = AuthUiState(
                    error = "Email and password cannot be empty"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = authRepository.login(email, password)) {

                is Result.Success -> {

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )

                    // register device for push notifications
                    registerFcmToken()
                }

                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Unexpected error"
                    )
                }
            }
        }
    }

    /* ---------------- SIGNUP ---------------- */

    fun signup(
        name: String,
        email: String,
        password: String,
        organisationId: String
    ) {
        viewModelScope.launch {

            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    error = "All fields are required"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            when (
                val result = authRepository.signup(
                    name,
                    email,
                    password,
                    organisationId
                )
            ) {

                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }

                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Unexpected error"
                    )
                }
            }
        }
    }

    /* ---------------- LOGOUT ---------------- */

    suspend fun logout() {
        try {

            // Delete the FCM token from Firebase
            FirebaseMessaging.getInstance().deleteToken()

        } catch (e: Exception) {
            Log.e("FCM", "Failed to delete FCM token", e)
        }

        // Clear local session
        tokenManager.clearAll()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun registerFcmToken() {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserIdBlocking()
                if (userId.isNullOrBlank()) return@launch

                val fcmToken =
                    com.google.firebase.messaging.FirebaseMessaging
                        .getInstance()
                        .token
                        .await()

                notificationApi.registerDevice(
                    com.docuvio.app.data.api.RegisterDeviceRequest(
                        userId = userId,
                        token = fcmToken,
                        platform = "android"
                    )
                )

                Log.d("FCM", "Token registered to backend")

            } catch (e: Exception) {
                Log.e("FCM", "Failed to register token", e)
            }
        }
    }
}
