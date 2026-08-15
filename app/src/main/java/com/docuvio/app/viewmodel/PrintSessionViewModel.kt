package com.docuvio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.data.model.*
import com.docuvio.app.data.repository.PrintSessionRepository
import com.docuvio.app.data.repository.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

data class PrintSessionUiState(
    val session: PrintSession? = null,
    val files: List<PrintSessionFile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val uploadProgress: Int? = null,
    val isUploading: Boolean = false,
    val paymentResult: String? = null
)

class PrintSessionViewModel(
    private val repository: PrintSessionRepository,
    private val tokenManager: TokenManager,
    private val initialToken: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrintSessionUiState())
    val uiState = _uiState.asStateFlow()

    private var realtimeJob: Job? = null
    private var pollingJob: Job? = null

    init {
        val token = initialToken ?: tokenManager.getGuestSessionTokenBlocking()
        if (token != null) {
            loadSession(token)
        }
    }

    private fun startPolling(token: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(4000) // Poll every 4 seconds
                val currentStatus = uiState.value.session?.status
                
                // Only poll if we are waiting for shop/payment/printing
                if (currentStatus != null && isPollingStatus(currentStatus)) {
                    val result = repository.getSession(token)
                    if (result is Result.Success) {
                        val updated = result.data.copy(token = token)
                        if (updated.status != currentStatus) {
                            _uiState.update { it.copy(session = updated) }
                            loadFiles(token)
                        }
                    }
                }
            }
        }
    }

    private fun isPollingStatus(status: PrintSessionStatus): Boolean {
        return when (status) {
            PrintSessionStatus.FILES_UPLOADED,
            PrintSessionStatus.REVIEWING,
            PrintSessionStatus.QUOTE_READY,
            PrintSessionStatus.PAYMENT_PENDING,
            PrintSessionStatus.PAID,
            PrintSessionStatus.PRINTING,
            PrintSessionStatus.READY_FOR_PICKUP -> true
            else -> false
        }
    }

    fun startSession(publicCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.startSession(publicCode)) {
                is Result.Success -> {
                    val session = result.data
                    _uiState.update { it.copy(session = session, isLoading = false) }
                    val token = session.activeToken
                    tokenManager.saveGuestSessionToken(token)
                    session.id?.let { startRealtimeObservation(it) }
                    if (token.isNotBlank()) {
                        loadFiles(token)
                        startPolling(token)
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    fun loadSession(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getSession(token)) {
                is Result.Success -> {
                    val session = result.data.copy(token = token)
                    _uiState.update { it.copy(session = session, isLoading = false) }
                    tokenManager.saveGuestSessionToken(token)
                    session.id?.let { startRealtimeObservation(it) }
                    loadFiles(token)
                    startPolling(token)
                    
                    if (session.status == PrintSessionStatus.CREATED) {
                        repository.connectSession(token)
                    }

                    if (session.status == PrintSessionStatus.CREATED || session.status == PrintSessionStatus.CONNECTED) {
                        if (session.customerName.isNullOrBlank()) {
                            tryAutoSubmitDetails()
                        }
                    }

                    // 🔥 ONLY CLEAR for terminal states
                    if (session.status == PrintSessionStatus.COMPLETED || 
                        session.status == PrintSessionStatus.EXPIRED) {
                        tokenManager.saveGuestSessionToken(null)
                    }

                    // ✅ Clear pending order ID if payment is confirmed
                    if (session.status == PrintSessionStatus.PAID || 
                        session.status == PrintSessionStatus.PRINTING ||
                        session.status == PrintSessionStatus.READY_FOR_PICKUP ||
                        session.status == PrintSessionStatus.COMPLETED) {
                        tokenManager.savePendingOrderId(null)
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                    if (result.message.contains("not found", true) || result.message.contains("expired", true)) {
                        tokenManager.saveGuestSessionToken(null)
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun startRealtimeObservation(sessionId: String) {
        realtimeJob?.cancel()
        realtimeJob = repository.observeSession(sessionId)
            .onEach { updatedSession ->
                val current = _uiState.value.session
                val mergedSession = updatedSession.copy(
                    token = updatedSession.token ?: current?.token,
                    sessionToken = updatedSession.sessionToken ?: current?.sessionToken,
                    shop = updatedSession.shop ?: current?.shop
                )
                
                _uiState.update { it.copy(session = mergedSession) }
                
                // 🔥 ONLY CLEAR for terminal states
                if (mergedSession.status == PrintSessionStatus.COMPLETED || 
                    mergedSession.status == PrintSessionStatus.EXPIRED) {
                    tokenManager.saveGuestSessionToken(null)
                }

                // ✅ Clear pending order ID if payment is confirmed
                if (mergedSession.status == PrintSessionStatus.PAID || 
                    mergedSession.status == PrintSessionStatus.PRINTING ||
                    mergedSession.status == PrintSessionStatus.READY_FOR_PICKUP ||
                    mergedSession.status == PrintSessionStatus.COMPLETED) {
                    tokenManager.savePendingOrderId(null)
                }

                mergedSession.activeToken.let { if (it.isNotBlank()) loadFiles(it) }
            }
            .catch { _ -> }
            .launchIn(viewModelScope)
    }

    fun loadFiles(token: String) {
        if (token.isBlank()) return
        viewModelScope.launch {
            when (val result = repository.getSessionFiles(token)) {
                is Result.Success -> _uiState.update { it.copy(files = result.data) }
                else -> {}
            }
        }
    }

    fun submitDetails(name: String, phone: String) {
        val session = uiState.value.session ?: return
        val token = session.activeToken
        if (token.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            tokenManager.saveGuestProfile(name, phone)
            when (val result = repository.submitCustomerDetails(token, name, phone)) {
                is Result.Success -> {
                    repository.startFileUploadPhase(token)
                    loadSession(token)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun tryAutoSubmitDetails() {
        val session = uiState.value.session ?: return
        val token = session.activeToken
        if (token.isBlank()) return

        viewModelScope.launch {
            var name = tokenManager.getUserNameBlocking()
            var phone = tokenManager.getPhoneBlocking()
            if (name.isNullOrBlank()) name = tokenManager.getGuestNameBlocking()
            if (phone.isNullOrBlank()) phone = tokenManager.getGuestPhoneBlocking()

            if (!name.isNullOrBlank() && !phone.isNullOrBlank()) {
                submitDetails(name, phone)
            }
        }
    }

    fun getPrefilledName(): String = runBlocking {
        tokenManager.getUserNameBlocking() ?: tokenManager.getGuestNameBlocking() ?: ""
    }

    fun getPrefilledPhone(): String = runBlocking {
        tokenManager.getPhoneBlocking() ?: tokenManager.getGuestPhoneBlocking() ?: ""
    }

    fun uploadFile(file: File, mimeType: String) {
        val token = uiState.value.session?.activeToken ?: return
        if (token.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0, error = null) }
            val result = repository.uploadFile(token, file, mimeType) { progress ->
                _uiState.update { it.copy(uploadProgress = progress) }
            }
            when (result) {
                is Result.Success -> {
                    loadFiles(token)
                    _uiState.update { it.copy(isUploading = false, uploadProgress = null) }
                    loadSession(token)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isUploading = false, uploadProgress = null) }
                }
                else -> {}
            }
        }
    }

    fun createPayment(onPaymentReady: (String, Int) -> Unit) {
        val token = uiState.value.session?.activeToken ?: return
        if (token.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.createPayment(token)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    val amountInRupees = result.data.amount
                    val orderId = result.data.orderId
                    if (orderId.isNotBlank()) {
                        // 💾 Persist for recovery if app is killed during payment
                        tokenManager.savePendingOrderId(orderId, "print_session")
                        onPaymentReady(orderId, amountInRupees)
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    fun setStatusLocally(status: PrintSessionStatus) {
        _uiState.update { state ->
            state.copy(session = state.session?.copy(status = status))
        }
    }

    fun dismissActiveSession() {
        val token = uiState.value.session?.activeToken ?: return
        viewModelScope.launch {
            tokenManager.dismissGuestSession(token)
        }
    }

    fun rejoinExistingSession() {
        val token = runBlocking { tokenManager.getGuestSessionTokenBlocking() }
        if (token != null) {
            loadSession(token)
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
        pollingJob?.cancel()
    }
}

class PrintSessionViewModelFactory(
    private val repository: PrintSessionRepository,
    private val tokenManager: TokenManager,
    private val initialToken: String? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PrintSessionViewModel(repository, tokenManager, initialToken) as T
    }
}
