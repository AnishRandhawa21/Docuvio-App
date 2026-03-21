package com.docuvio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.data.model.RazorpayHolder
import com.docuvio.app.data.repository.OrderRepository
import com.docuvio.app.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class WalkInOrderStage {
    Idle,
    CreatingOrder,
    UploadingFile,
    AttachingDocument,
    CreatingPayment,
    WaitingForPayment,
    VerifyingPayment,
}

data class WalkInOrderUiState(
    val stage: WalkInOrderStage = WalkInOrderStage.Idle,
    val pageCount: Int = 0,
    val uploadProgress: Int = 0,
    val selectedFile: File? = null,
    val selectedFileMimeType: String = "application/octet-stream",  // ← MIME carried through upload
    val amount: String = "",
    val notes: String = "",
    val error: String? = null,
    val isSuccess: Boolean = false
) {
    val isLoading: Boolean get() = stage != WalkInOrderStage.Idle && stage != WalkInOrderStage.WaitingForPayment
    val uploadingFile: Boolean get() = stage == WalkInOrderStage.UploadingFile
    val isWaitingForPayment: Boolean get() = stage == WalkInOrderStage.WaitingForPayment
}

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

    // Called from UI after file is copied to cache — receives resolved, normalised MIME
    fun setFile(file: File, pageCount: Int, mimeType: String) {
        val canonical = normaliseMime(mimeType)
        val correctExt = mimeToExtension(canonical)

        // Ensure filename extension matches the real type
        val renamedFile = if (file.extension.lowercase() != correctExt) {
            File(file.parent, "upload_${System.currentTimeMillis()}.$correctExt")
                .also { dest -> file.renameTo(dest) }
        } else file

        _uiState.value = _uiState.value.copy(
            selectedFile = renamedFile,
            selectedFileMimeType = canonical,
            pageCount = pageCount
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, stage = WalkInOrderStage.Idle)
    }

    fun resetState() {
        _uiState.value = WalkInOrderUiState()
    }

    fun submitOrder(onPaymentRequired: (String, Int) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value

            val file = state.selectedFile ?: run {
                _uiState.value = state.copy(error = "Please upload a document")
                return@launch
            }

            val price = state.amount.toIntOrNull()
            if (price == null || price <= 0) {
                _uiState.value = state.copy(error = "Invalid amount")
                return@launch
            }

            /* 1️⃣ CREATE WALK-IN ORDER */
            _uiState.value = _uiState.value.copy(stage = WalkInOrderStage.CreatingOrder)

            val orderResult = orderRepository.createWalkInOrder(
                shopId = shopId,
                notes = state.notes
            )

            if (orderResult is Result.Error) {
                _uiState.value = _uiState.value.copy(
                    stage = WalkInOrderStage.Idle,
                    error = orderResult.message
                )
                return@launch
            }

            val orderId = (orderResult as Result.Success).data.id
            currentOrderId = orderId

            /* 2️⃣ UPLOAD FILE — pass resolved MIME type so backend gets correct Content-Type */
            _uiState.value = _uiState.value.copy(
                stage = WalkInOrderStage.UploadingFile,
                uploadProgress = 0
            )

            val uploadResult = orderRepository.uploadFile(
                file = file,
                mimeType = state.selectedFileMimeType,  // ← THE FIX
                onProgress = { percent ->
                    _uiState.value = _uiState.value.copy(uploadProgress = percent)
                }
            )

            if (uploadResult is Result.Error) {
                _uiState.value = _uiState.value.copy(
                    stage = WalkInOrderStage.Idle,
                    error = uploadResult.message
                )
                return@launch
            }

            val fileKey = (uploadResult as Result.Success).data.fileKey

            /* 3️⃣ ATTACH DOCUMENT */
            _uiState.value = _uiState.value.copy(stage = WalkInOrderStage.AttachingDocument)

            val attachResult = orderRepository.attachWalkInDocument(
                orderId = orderId,
                fileKey = fileKey,
                fileName = file.name,
                manualPrice = price
            )

            if (attachResult is Result.Error) {
                _uiState.value = _uiState.value.copy(
                    stage = WalkInOrderStage.Idle,
                    error = attachResult.message
                )
                return@launch
            }

            /* 4️⃣ CREATE PAYMENT */
            _uiState.value = _uiState.value.copy(stage = WalkInOrderStage.CreatingPayment)

            val paymentResult = orderRepository.createPayment(orderId)

            if (paymentResult is Result.Error) {
                _uiState.value = _uiState.value.copy(
                    stage = WalkInOrderStage.Idle,
                    error = paymentResult.message
                )
                return@launch
            }

            if (paymentResult is Result.Success) {
                val razorpayOrderId = paymentResult.data.id
                val amount = paymentResult.data.amount

                if (!razorpayOrderId.isNullOrBlank() && amount != null) {
                    _uiState.value = _uiState.value.copy(stage = WalkInOrderStage.WaitingForPayment)
                    onPaymentRequired(razorpayOrderId, amount)
                } else {
                    _uiState.value = _uiState.value.copy(
                        stage = WalkInOrderStage.Idle,
                        error = "Payment data missing"
                    )
                }
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
            _uiState.value = _uiState.value.copy(stage = WalkInOrderStage.VerifyingPayment)

            val result = orderRepository.verifyPayment(
                razorpayOrderId,
                razorpayPaymentId,
                razorpaySignature,
                orderId
            )

            if (result is Result.Success) {
                _uiState.value = _uiState.value.copy(
                    stage = WalkInOrderStage.Idle,
                    isSuccess = true
                )
            } else if (result is Result.Error) {
                _uiState.value = _uiState.value.copy(
                    stage = WalkInOrderStage.Idle,
                    error = result.message
                )
            }
        }
    }

    fun setPaymentCancelled() {
        _uiState.value = _uiState.value.copy(
            stage = WalkInOrderStage.Idle,
            error = "Payment cancelled"
        )
    }

    /* ---------------- PRIVATE HELPERS ---------------- */

    private fun normaliseMime(mime: String): String = when (mime.lowercase().trim()) {
        "image/jpg", "image/pjpeg" -> "image/jpeg"
        else -> mime
    }

    private fun mimeToExtension(mime: String): String = when (mime) {
        "image/jpeg"      -> "jpg"
        "image/png"       -> "png"
        "application/pdf" -> "pdf"
        else              -> "bin"
    }
}