package com.docuvio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.data.model.*
import com.docuvio.app.data.repository.OrderRepository
import com.docuvio.app.data.repository.Result
import com.docuvio.app.data.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import com.docuvio.app.BuildConfig
import com.docuvio.app.ui.order.utils.DocxConverter
import com.docuvio.app.utils.PdfUtils
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.docuvio.app.ui.order.utils.PricingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody

/* ---------------- UI STATE ---------------- */

data class CreateOrderUiState(
    val isLoading: Boolean = false,
    val isConverting: Boolean = false,        // ← ADD: shows spinner on file box
    val conversionError: String? = null,
    val printOptions: PrintOptions? = null,
    val isCvMode: Boolean = false,
    val shop: Shop? = null,
    val description: String = "",
    val selectedFile: File? = null,
    val selectedFileMimeType: String = "application/octet-stream",  // ← MIME carried through upload
    val selectedPaperType: PaperType? = null,
    val selectedColorMode: ColorMode? = null,
    val selectedFinishType: FinishType? = null,
    val printSide: String = "single", // "single" or "double"
    val pageCount: Int = 1,
    val copies: Int = 1,
    val orientation: PrintOrientation = PrintOrientation.PORTRAIT,
    val pickupAt: String? = null,
    val isHandled: Boolean = false,
    val handlingFee: Int = 0,
    val uploadProgress: Int = 0,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val currentStep: OrderStep = OrderStep.LOADING_OPTIONS,
    val documentPrice: Int = 0,
    val platformFee: Int = 0,
    val totalAmount: Int = 0,
)

enum class OrderStep {
    LOADING_OPTIONS,
    SELECT_OPTIONS,
    CREATING_ORDER,
    UPLOADING,
    ATTACHING_DOCUMENT,
    PROCESSING_PAYMENT,
    SUCCESS
}

/* ---------------- VIEWMODEL ---------------- */

class CreateOrderViewModel(
    private val shopRepository: ShopRepository,
    private val orderRepository: OrderRepository,
    private val shopId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    private var currentOrderId: String? = null
    private var orderJob: Job? = null

    init {
        loadShop()
        loadPrintOptions()
    }

    /* ---------------- LOAD OPTIONS ---------------- */

    private fun loadPrintOptions() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                when (val result = shopRepository.getPrintOptions(shopId)) {
                    is Result.Success -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        printOptions = result.data,
                        selectedPaperType = null,
                        selectedColorMode = result.data.colorModes.firstOrNull(),
                        selectedFinishType = null,
                        currentStep = OrderStep.SELECT_OPTIONS
                    )
                    is Result.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        currentStep = OrderStep.SELECT_OPTIONS
                    )
                    is Result.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load print options",
                    currentStep = OrderStep.SELECT_OPTIONS
                )
            }
        }
    }

    /* ---------------- STATE SETTERS ---------------- */

    fun setPaperType(paperType: PaperType) {
        _uiState.update { it.copy(selectedPaperType = paperType) }
        recalculatePricing()
    }

    fun setColorMode(colorMode: ColorMode) {
        _uiState.update { it.copy(selectedColorMode = colorMode) }
        recalculatePricing()
    }
    fun setFinishType(finishType: FinishType) {
        _uiState.update { it.copy(selectedFinishType = finishType) }
        recalculatePricing()
    }

    fun setCopies(copies: Int) {
        _uiState.update { it.copy(copies = copies) }
        recalculatePricing()
    }

    fun setPrintSide(side: String) {
        _uiState.update { it.copy(printSide = side) }
        recalculatePricing()
    }
    fun setOrientation(orientation: PrintOrientation) { _uiState.value = _uiState.value.copy(orientation = orientation) }
    fun setDescription(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun setError(message: String) { _uiState.update { it.copy(error = message) } }

    fun setPickupAt(pickupAtIso: String) {
        val handled = isTomorrowPickup(pickupAtIso)
        _uiState.update {
            it.copy(
                pickupAt = pickupAtIso,
                isHandled = handled,
                handlingFee = if (handled) 10 else 0
            )
        }
        recalculatePricing()
    }

    /* ---------------- FILE READ ---------------- */

    fun setFileAndReadPages(file: File, mimeType: String) {
        val canonical = normaliseMime(mimeType)
        val correctExt = mimeToExtension(canonical)

        val renamedFile = if (file.extension.lowercase() != correctExt) {
            File(file.parent, "upload_${System.currentTimeMillis()}.$correctExt")
                .also { dest -> file.renameTo(dest) }
        } else file

        val isDocx = renamedFile.extension.lowercase() == "docx" ||
                canonical.contains("word") ||
                canonical.contains("officedocument")

        if (isDocx) {
            // Show spinner on file box immediately, clear any old file
            _uiState.update {
                it.copy(
                    isConverting = true,
                    conversionError = null,
                    selectedFile = null,       // clear old preview while converting
                    pageCount = 1
                )
            }
        } else {
            // Non-DOCX: just store the file directly, no conversion needed
            _uiState.update {
                it.copy(
                    selectedFile = renamedFile,
                    selectedFileMimeType = canonical
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (finalFile, finalMime) = if (isDocx) {
                    val converted = DocxConverter.convertToPdf(renamedFile)
                    Pair(converted, "application/pdf")
                } else {
                    Pair(renamedFile, canonical)
                }

                val pageCount = if (finalMime == "application/pdf")
                    PdfUtils.getPdfPageCount(finalFile) else 1

                _uiState.update {
                    it.copy(
                        isConverting = false,
                        conversionError = null,
                        selectedFile = finalFile,
                        selectedFileMimeType = finalMime,
                        pageCount = pageCount
                    )
                }
                recalculatePricing()

            } catch (e: Exception) {
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

    /* ---------------- ORDER FLOW ---------------- */

    fun submitOrder(onPaymentRequired: (String, Int) -> Unit) {
        orderJob?.cancel()
        orderJob = viewModelScope.launch {
            val state = _uiState.value

            val shop = state.shop ?: run {
                _uiState.value = state.copy(error = "Shop unavailable")
                return@launch
            }

            if (!canCreateOrderNow(shop, state.pickupAt)) {
                _uiState.value = state.copy(error = "Orders are closed for today. Please try again later.")
                return@launch
            }

            val file = state.selectedFile ?: run {
                _uiState.value = state.copy(error = "Please select a file")
                return@launch
            }

            val paper = state.selectedPaperType
            val color = state.selectedColorMode
            val finish = state.selectedFinishType

            if (paper == null || color == null || finish == null) {
                _uiState.value = state.copy(error = "Please select print options first")
                return@launch
            }

            /* 1️⃣ CREATE ORDER */
            _uiState.value = state.copy(
                isLoading = true,
                currentStep = OrderStep.CREATING_ORDER
            )

            val orderResult = orderRepository.createOrder(
                shopId = shopId,
                description = state.description.ifBlank { "Print order" },
                orientation = state.orientation,
                pickupAt = state.pickupAt,
                isHandled = state.isHandled
            )

            if (orderResult !is Result.Success) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = (orderResult as Result.Error).message,
                    currentStep = OrderStep.SELECT_OPTIONS
                )
                return@launch
            }

            val orderId = orderResult.data.id
            currentOrderId = orderId

            /* 2️⃣ UPLOAD FILE — pass resolved MIME type so backend gets correct Content-Type */
            _uiState.value = _uiState.value.copy(
                currentStep = OrderStep.UPLOADING,
                uploadProgress = 0
            )

            val uploadResult = orderRepository.uploadFile(
                file = file,
                mimeType = state.selectedFileMimeType,  // ← THE FIX
                onProgress = { percent ->
                    _uiState.value = _uiState.value.copy(uploadProgress = percent)
                }
            )

            if (uploadResult !is Result.Success) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = (uploadResult as Result.Error).message,
                    currentStep = OrderStep.SELECT_OPTIONS
                )
                return@launch
            }

            val fileKey = uploadResult.data.fileKey
            if (fileKey.isBlank()) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = "File upload failed. Please try again.",
                    currentStep = OrderStep.SELECT_OPTIONS
                )
                return@launch
            }

            /* 3️⃣ ATTACH DOCUMENT */
            _uiState.value = _uiState.value.copy(currentStep = OrderStep.ATTACHING_DOCUMENT)

            val attachResult = orderRepository.attachDocument(
                orderId = orderId,
                fileKey = fileKey,
                fileName = file.name,
                pageCount = state.pageCount,
                copies = state.copies,
                paperTypeId = paper.id,
                colorModeId = color.id,
                finishTypeId = finish.id,
                pickupAt = state.pickupAt,
                isHandled = state.isHandled,
                printSide = state.printSide
            )

            if (attachResult !is Result.Success) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = (attachResult as Result.Error).message,
                    currentStep = OrderStep.SELECT_OPTIONS
                )
                return@launch
            }

            kotlinx.coroutines.delay(1500)

            /* 4️⃣ CREATE PAYMENT */
            _uiState.value = _uiState.value.copy(currentStep = OrderStep.PROCESSING_PAYMENT)

            kotlinx.coroutines.delay(1500)

            val paymentResult = orderRepository.createPayment(orderId)

            if (paymentResult is Result.Success) {
                val razorpayOrderId = paymentResult.data.id
                val amount = paymentResult.data.amount
                if (razorpayOrderId.isNullOrBlank() || amount == null || amount == 0) {
                    _uiState.value = state.copy(
                        isLoading = false,
                        error = "Payment creation failed",
                        currentStep = OrderStep.SELECT_OPTIONS
                    )
                    return@launch
                }
                onPaymentRequired(razorpayOrderId, amount)
            } else {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = (paymentResult as Result.Error).message,
                    currentStep = OrderStep.SELECT_OPTIONS
                )
            }
        }
    }

    /* ---------------- VERIFY PAYMENT ---------------- */

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
                    isSuccess = true,
                    currentStep = OrderStep.SUCCESS
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = (result as Result.Error).message
                )
            }
        }
    }

    fun setPaymentCancelled() {
        orderJob?.cancel()
        _uiState.update { it.copy(currentStep = OrderStep.SELECT_OPTIONS, error = "Payment cancelled") }
    }

    /* ---------------- CV MODE ---------------- */

    fun enableCvMode() {
        val options = _uiState.value.printOptions ?: return

        val bondPaper = options.paperTypes.find { it.name.lowercase() == "bond" }
        val bondColor = options.colorModes.find { it.name.lowercase() == "bond" }
        val standardFinish = options.finishTypes.find { it.name.lowercase() == "standard" }
            ?: options.finishTypes.firstOrNull { it.name.lowercase() != "bond" }

        if (bondPaper == null) {
            _uiState.update { it.copy(error = "CV printing not available at this shop") }
            return
        }

        _uiState.update {
            it.copy(
                isCvMode = true,
                selectedPaperType = bondPaper,
                selectedColorMode = bondColor ?: it.selectedColorMode,
                selectedFinishType = standardFinish ?: it.selectedFinishType,
            )
        }
        recalculatePricing()
    }

    fun disableCvMode() {
        _uiState.update {
            it.copy(
                isCvMode = false,
                selectedPaperType = null,
                selectedFinishType = null,
                selectedColorMode = it.printOptions
                    ?.colorModes
                    ?.firstOrNull { cm -> cm.name.lowercase() != "bond" },
            )
        }
        recalculatePricing()
    }

    fun toggleCvMode() {
        if (_uiState.value.isCvMode) disableCvMode() else enableCvMode()
    }

    /* ---------------- PRIVATE HELPERS ---------------- */

    private fun normaliseMime(mime: String): String = when (mime.lowercase().trim()) {
        "image/jpg", "image/JPEG" -> "image/jpeg"
        else -> mime
    }

    private fun mimeToExtension(mime: String): String = when (mime) {
        "image/jpeg"      -> "jpg"
        "image/png"       -> "png"
        "application/pdf" -> "pdf"
        else              -> "bin"
    }

    private fun isTomorrowPickup(pickupAtIso: String): Boolean {
        val date = parseIsoDate(pickupAtIso)
        val pickupCal = Calendar.getInstance().apply { time = date }
        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        return pickupCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                pickupCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
    }

    private fun pickupMinutes(pickupAtIso: String): Int {
        val cal = Calendar.getInstance().apply { time = parseIsoDate(pickupAtIso) }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun canCreateOrderNow(shop: Shop, pickupAtIso: String?): Boolean {
        if (!shop.isAcceptingOrder) return false
        if (pickupAtIso == null) return true
        val open = shop.openTime.toMinutes()
        val last = shop.closeTime.toMinutes() - 30
        return pickupMinutes(pickupAtIso) in open until last
    }

    private fun loadShop() {
        viewModelScope.launch {
            when (val result = shopRepository.getShop(shopId)) {
                is Result.Success -> _uiState.update { it.copy(shop = result.data) }
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
        }
    }

    private fun parseIsoDate(iso: String): Date {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in formats) {
            try { return SimpleDateFormat(pattern, Locale.getDefault()).parse(iso)!! } catch (_: Exception) {}
        }
        throw IllegalArgumentException("Invalid ISO date: $iso")
    }

    private fun String.toMinutes(): Int = try {
        val p = split(":"); p[0].toInt() * 60 + p[1].toInt()
    } catch (_: Exception) { 0 }

    private fun recalculatePricing() {
        val state = _uiState.value
        val docPrice  = PricingUtils.calculateDocumentPrice(state)
        val platform  = PricingUtils.calculatePlatformFee(docPrice)
        val handling  = PricingUtils.calculateHandlingFee(state)
        _uiState.update {
            it.copy(
                documentPrice = docPrice,
                platformFee   = platform,
                handlingFee   = handling,
                totalAmount   = docPrice + platform + handling
            )
        }
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
}
