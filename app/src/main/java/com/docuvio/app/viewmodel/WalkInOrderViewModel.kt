package com.docuvio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.data.repository.OrderRepository
import com.docuvio.app.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class WalkInOrderUiState(
    val isLoading: Boolean = false,
    val pageCount: Int = 0,
    val uploadingFile: Boolean = false,
    val uploadProgress: Int = 0,
    val selectedFile: File? = null,
    val amount: String = "",
    val notes: String = "",
    val error: String? = null,
    val isSuccess: Boolean = false
)

class WalkInOrderViewModel(
    private val orderRepository: OrderRepository,
    private val shopId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalkInOrderUiState())
    val uiState: StateFlow<WalkInOrderUiState> = _uiState.asStateFlow()

    private var currentOrderId: String? = null

    fun setAmount(value: String) {
        _uiState.value = _uiState.value.copy(amount = value)
    }

    fun setNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun setFile(file: File, pageCount: Int) {
        _uiState.value = _uiState.value.copy(
            selectedFile = file,
            pageCount = pageCount
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    fun resetState() {
        _uiState.value = WalkInOrderUiState()
    }

    fun submitOrder(onPaymentRequired: (String, Int) -> Unit) {

        viewModelScope.launch {

            val state = _uiState.value

            val file = state.selectedFile ?: run {
                _uiState.value = state.copy(error = "Please upload a PDF")
                return@launch
            }

            val price = state.amount.toIntOrNull()

            if (price == null || price <= 0) {
                _uiState.value = state.copy(error = "Invalid amount")
                return@launch
            }

            _uiState.value = state.copy(isLoading = true)

            /* 1️⃣ CREATE WALK-IN ORDER */

            val orderResult = orderRepository.createWalkInOrder(
                shopId = shopId,
                notes = state.notes
            )

            if (orderResult is Result.Error) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = orderResult.message
                )
                return@launch
            }

            val orderId = (orderResult as Result.Success).data.id
            currentOrderId = orderId

            /* 2️⃣ UPLOAD FILE */

            _uiState.value = _uiState.value.copy(
                uploadingFile = true,
                uploadProgress = 0
            )

            val uploadResult = orderRepository.uploadFile(
                file = file,
                onProgress = { percent ->
                    _uiState.value = _uiState.value.copy(
                        uploadProgress = percent
                    )
                }
            )

            _uiState.value = _uiState.value.copy(uploadingFile = false)

            if (uploadResult is Result.Error) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = uploadResult.message
                )
                return@launch
            }

            val fileKey = (uploadResult as Result.Success).data.fileKey

            /* 3️⃣ ATTACH DOCUMENT */

            val attachResult =
                orderRepository.attachWalkInDocument(
                    orderId = orderId,
                    fileKey = fileKey,
                    fileName = file.name,
                    manualPrice = price
                )

            if (attachResult is Result.Error) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = attachResult.message
                )
                return@launch
            }

            /* 4️⃣ CREATE PAYMENT */

            val paymentResult = orderRepository.createPayment(orderId)

            if (paymentResult is Result.Success) {

                val razorpayOrderId = paymentResult.data.id
                val amount = paymentResult.data.amount

                if (!razorpayOrderId.isNullOrBlank() && amount != null) {
                    onPaymentRequired(razorpayOrderId, amount)
                }

            } else if (paymentResult is Result.Error) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = paymentResult.message
                )
            }
        }
    }

    fun verifyPayment(
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String
    ) {

        val orderId = currentOrderId ?: return

        viewModelScope.launch {

            val result = orderRepository.verifyPayment(
                razorpayOrderId,
                razorpayPaymentId,
                razorpaySignature,
                orderId
            )

            if (result is Result.Success) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )

            } else if (result is Result.Error) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }
    fun setPaymentCancelled() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = "Payment cancelled"
        )
    }
}