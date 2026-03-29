package com.docuvio.app.data.repository

import android.util.Log
import com.docuvio.app.data.api.OrderApi
import com.docuvio.app.data.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OrderRepository(
    private val orderApi: OrderApi
) {

    /* ---------------- CREATE ORDER ---------------- */

    suspend fun createOrder(
        shopId: String,
        description: String,
        orientation: PrintOrientation,
        pickupAt: String?,
        isHandled: Boolean
    ): Result<CreateOrderResponse> {
        return try {
            val response = orderApi.createOrder(
                CreateOrderRequest(
                    shopId = shopId,
                    description = description,
                    orientation = orientation,
                    pickupAt = pickupAt,
                    isHandled = isHandled
                )
            )
            if (!response.isSuccessful) {
                return mapErrorResponse(response.code())
            }
            val body = response.body() ?: return Result.Error("Empty server response.")
            Result.Success(body.data)
        } catch (e: Exception) {
            Log.e("ORDER_REPO", "Create order exception: ${e.message}", e)
            Result.Error(mapNetworkError(e))
        }
    }

    /* ---------------- UPLOAD FILE (with progress) ---------------- */

    suspend fun uploadFile(
        file: File,
        mimeType: String,
        onProgress: (Int) -> Unit
    ): Result<UploadData> {
        return try {
            val resolvedType = mimeType.toMediaTypeOrNull()
                ?: "application/octet-stream".toMediaTypeOrNull()

            val requestBody = object : RequestBody() {

                override fun contentType() = resolvedType

                override fun contentLength() = file.length()

                override fun writeTo(sink: BufferedSink) {
                    file.source().use { source ->
                        val buffer = Buffer()
                        var totalBytes = 0L
                        val fileLength = file.length()
                        var read: Long
                        while (source.read(buffer, 8_192).also { read = it } != -1L) {
                            sink.write(buffer, read)
                            totalBytes += read
                            val progress = if (fileLength > 0) ((totalBytes * 100) / fileLength).toInt() else 0
                            onProgress(progress)
                        }
                    }
                }
            }

            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val response = orderApi.uploadFile(part)

            if (!response.isSuccessful) {
                return mapErrorResponse(response.code())
            }

            val data = response.body()?.data ?: return Result.Error("Empty upload response")
            Result.Success(data)
        } catch (e: Exception) {
            Log.e("ORDER_REPO", "Upload file exception: ${e.message}", e)
            Result.Error(mapNetworkError(e))
        }
    }

    /* ---------------- ATTACH DOCUMENT ---------------- */

    suspend fun attachDocument(
        orderId: String,
        fileKey: String,
        fileName: String,
        pageCount: Int,
        copies: Int,
        paperTypeId: String,
        colorModeId: String,
        finishTypeId: String,
        pickupAt: String?,
        printSide: String,
        isHandled: Boolean
    ): Result<Unit> {
        return try {
            val response = orderApi.attachDocument(
                orderId,
                AttachDocumentRequest(
                    fileKey = fileKey,
                    fileName = fileName,
                    pageCount = pageCount,
                    copies = copies,
                    paperTypeId = paperTypeId,
                    colorModeId = colorModeId,
                    finishTypeId = finishTypeId,
                    pickupAt = pickupAt,
                    printSide = printSide
                )
            )
            if (!response.isSuccessful) {
                return mapErrorResponse(response.code())
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("ORDER_REPO", "Attach document exception: ${e.message}", e)
            Result.Error(mapNetworkError(e))
        }
    }

    /* ---------------- GET ORDERS ---------------- */

    suspend fun getOrders(): Result<OrdersResponse> {
        return try {
            val response = orderApi.getOrders()
            if (!response.isSuccessful) {
                return mapErrorResponse(response.code())
            }
            val body = response.body() ?: return Result.Error("Empty response")
            Result.Success(body)
        } catch (e: Exception) {
            Log.e("ORDER_REPO", "Get orders exception: ${e.message}", e)
            Result.Error(mapNetworkError(e))
        }
    }

    /* ---------------- PAYMENT ---------------- */

    suspend fun createPayment(orderId: String): Result<CreatePaymentResponse> {
        return try {
            val response = orderApi.createPayment(CreatePaymentRequest(orderId))
            if (!response.isSuccessful) {
                return mapErrorResponse(response.code())
            }

            val apiResponse = response.body()
            if (apiResponse == null || !apiResponse.success) {
                return Result.Error(apiResponse?.message ?: "Empty payment response")
            }

            val body = apiResponse.data
            if (body.id.isNullOrBlank() || body.amount == null || body.amount == 0) {
                return Result.Error("Invalid payment response from server")
            }

            Result.Success(body)
        } catch (e: Exception) {
            Log.e("ORDER_REPO", "Create payment exception: ${e.message}", e)
            Result.Error(mapNetworkError(e))
        }
    }

    suspend fun verifyPayment(
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String,
        orderId: String
    ): Result<VerifyPaymentResponse> {
        return try {
            val response = orderApi.verifyPayment(
                VerifyPaymentRequest(razorpayOrderId, razorpayPaymentId, razorpaySignature, orderId)
            )
            if (!response.isSuccessful) {
                return mapErrorResponse(response.code())
            }
            val body = response.body() ?: return Result.Error("Empty verification response")
            Result.Success(body)
        } catch (e: Exception) {
            Log.e("ORDER_REPO", "Verify payment exception: ${e.message}", e)
            Result.Error(mapNetworkError(e))
        }
    }

    /* ---------------- WALK-IN ---------------- */

    suspend fun createWalkInOrder(shopId: String, notes: String?): Result<CreateOrderResponse> {
        return try {
            val response = orderApi.createWalkInOrder(WalkInOrderRequest(shopId = shopId, notes = notes))
            if (!response.isSuccessful) return mapErrorResponse(response.code())
            val body = response.body() ?: return Result.Error("Empty response")
            Result.Success(body.data)
        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    suspend fun attachWalkInDocument(
        orderId: String,
        fileKey: String,
        fileName: String,
        manualPrice: Int
    ): Result<Unit> {
        return try {
            val response = orderApi.attachWalkInDocument(
                orderId,
                AttachWalkInDocument(fileKey, fileName, 1, manualPrice)
            )
            if (!response.isSuccessful) return mapErrorResponse(response.code())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    /* ---------------- ERROR MAPPERS ---------------- */

    private fun <T> mapErrorResponse(code: Int): Result<T> {
        return when (code) {
            401, 403 -> Result.Error("Session expired. Please login again.")
            404      -> Result.Error("Resource not found.")
            413      -> Result.Error("File too large.")
            429      -> Result.Error("Too many requests. Please slow down.")
            500, 502, 503 -> Result.Error("Server error. Please try again later.")
            else     -> Result.Error("Something went wrong (Error $code)")
        }
    }

    private fun mapNetworkError(e: Exception): String {
        return when (e) {
            is UnknownHostException  -> "No internet connection."
            is SocketTimeoutException -> "Connection timed out. Please try again."
            is IOException           -> "Network error. Please check your connection."
            else                     -> "Something went wrong. Please try again."
        }
    }
}
