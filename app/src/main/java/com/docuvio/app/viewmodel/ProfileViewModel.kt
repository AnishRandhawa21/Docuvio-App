package com.docuvio.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
) : ViewModel(){

    var isLoggingOut by mutableStateOf(false)
        private set

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            isLoggingOut = true
            authRepository.logout()
            isLoggingOut = false
            onLogoutComplete()
        }
    }

}