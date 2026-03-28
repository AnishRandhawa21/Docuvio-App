package com.docuvio.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.BuildConfig
import com.docuvio.app.data.repository.OrderRepository
import com.docuvio.app.data.repository.Result
import com.docuvio.app.ui.order.utils.DocxConverter
import com.docuvio.app.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody

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
    val isConverting: Boolean = false,        // ← ADD
    val conversionError: String? = null,
    val uploadProgress: Int = 0,
    val selectedFile: File? = null,
    val selectedFileMimeType: String = "application/octet-stream",  // ← MIME carried through upload
    val amount: String = "",
    val notes: String = "",
    val error: String? = null,
    val isSuccess: Boolean = false
) {
//    val isLoading: Boolean get() = stage != WalkInOrderStage.Idle && stage != WalkInOrderStage.WaitingForPayment
//    val uploadingFile: Boolean get() = stage == WalkInOrderStage.UploadingFile
//    val isWaitingForPayment: Boolean get() = stage == WalkInOrderStage.WaitingForPayment
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

    fun setFile(file: File, mimeType: String) {
        val canonical = normaliseMime(mimeType)
        Log.d("WalkInVM", "setFile called — file: ${file.name}, size: ${file.length()}, mime: $canonical")

        val isDocx = file.extension.lowercase() == "docx" ||
                canonical.contains("word") ||
                canonical.contains("officedocument")

        Log.d("WalkInVM", "isDocx: $isDocx (extension=${file.extension}, mime=$canonical)")

        if (isDocx) {
            Log.d("WalkInVM", "DOCX detected — setting isConverting = true")
            _uiState.update {
                it.copy(
                    isConverting = true,
                    conversionError = null,
                    selectedFile = null,
                    pageCount = 0
                )
            }
        } else {
            Log.d("WalkInVM", "Non-DOCX — storing file directly")
            _uiState.update {
                it.copy(
                    selectedFile = file,
                    selectedFileMimeType = canonical
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (finalFile, finalMime) = if (isDocx) {
                    Log.d("WalkInVM", "Starting DOCX → PDF conversion")
                    val converted = DocxConverter.convertToPdf(file)
                    Log.d("WalkInVM", "Conversion done — PDF: ${converted.absolutePath}, size: ${converted.length()}")
                    Pair(converted, "application/pdf")
                } else {
                    Pair(file, canonical)
                }

                val pageCount = if (finalMime == "application/pdf") {
                    val count = PdfUtils.getPdfPageCount(finalFile)
                    Log.d("WalkInVM", "Page count: $count")
                    count
                } else 1

                Log.d("WalkInVM", "Updating UI — file ready, isConverting = false")
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        conversionError = null,
                        selectedFile = finalFile,
                        selectedFileMimeType = finalMime,
                        pageCount = pageCount
                    )
                }

            } catch (e: Exception) {
                Log.e("WalkInVM", "Conversion/processing failed", e)
                Log.e("WalkInVM", "Exception type: ${e.javaClass.name}")
                Log.e("WalkInVM", "Exception message: ${e.message}")
                e.cause?.let { Log.e("WalkInVM", "Caused by: ${it.javaClass.name}: ${it.message}") }

                _uiState.update {
                    it.copy(
                        isConverting = false,
                        selectedFile = null,
                        conversionError = "Could not convert file: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun clearConversionError() {
        _uiState.update { it.copy(conversionError = null) }
    }

    fun convertDocxToPdf(file: File): File {

        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()

        val url = "${BuildConfig.CONVERTER_URL}/convert"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", BuildConfig.CONVERTER_API_KEY)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Conversion failed: ${response.code}")
        }

        val pdfFile = File(
            file.parent,
            file.name.substringBeforeLast(".") + ".pdf"
        )

        response.body?.byteStream()?.use { input ->
            pdfFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Empty response body")

        return pdfFile
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
        "image/jpg", "image/JPEG" -> "image/jpeg"
        else -> mime
    }

}