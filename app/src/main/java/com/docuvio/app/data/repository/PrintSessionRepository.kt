package com.docuvio.app.data.repository

import com.docuvio.app.data.api.PrintSessionApi
import com.docuvio.app.data.api.StartUploadRequest
import com.docuvio.app.data.model.*
import com.google.gson.Gson
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.source
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PrintSessionRepository(
    private val api: PrintSessionApi,
    private val supabase: SupabaseClient
) {

    suspend fun startSession(publicCode: String): Result<PrintSession> {
        return try {
            val response = api.startSession(publicCode)
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error("Empty server response")
                val startData = body.data ?: return Result.Error("Session creation failed")
                
                val token = startData.sessionToken
                if (token.isNullOrBlank()) {
                    return Result.Error("Session token missing from server")
                }

                val session = startData.session?.copy(token = token) ?: PrintSession(token = token)
                Result.Success(session)
            } else {
                handleErrorResponse(response)
            }
        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    suspend fun connectSession(sessionToken: String): Result<Unit> {
        return try {
            val response = api.connectSession(sessionToken)
            if (response.isSuccessful) Result.Success(Unit)
            else handleErrorResponse(response)
        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    suspend fun getSession(sessionToken: String): Result<PrintSession> {
        return try {
            val response = api.getSession(sessionToken)
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error("Empty server response")
                val data = body.data ?: return Result.Error("Missing data from server")
                Result.Success(data)
            } else {
                handleErrorResponse(response)
            }
        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    suspend fun submitCustomerDetails(sessionToken: String, name: String, phone: String): Result<Unit> {
        return try {
            val response = api.submitCustomerDetails(sessionToken, CustomerDetailsRequest(name, phone))
            if (response.isSuccessful) Result.Success(Unit)
            else handleErrorResponse(response)
        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    suspend fun startFileUploadPhase(sessionToken: String): Result<PrintSession> {
        return try {
            val response = api.startFileUploadPhase(sessionToken)
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error("Empty server response")
                val data = body.data ?: return Result.Error("Missing session data")
                Result.Success(data)
            } else {
                handleErrorResponse(response)
            }
        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    suspend fun uploadFile(
        sessionToken: String,
        file: File,
        mimeType: String,
        onProgress: (Int) -> Unit
    ): Result<PrintSessionFile> {
        return try {
            api.startFileUpload(sessionToken, StartUploadRequest(file.name, mimeType))

            val resolvedType = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
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
            val response = api.uploadFile(sessionToken, part)

            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error("Empty server response")
                val data = body.data ?: return Result.Error("Missing data from server")
                Result.Success(data)
            } else {
                handleErrorResponse(response)
            }
        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    suspend fun getSessionFiles(sessionToken: String): Result<List<PrintSessionFile>> {
        return try {
            val response = api.getSessionFiles(sessionToken)
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error("Empty server response")
                val data = body.data ?: return Result.Error("Missing data from server")
                Result.Success(data)
            } else {
                handleErrorResponse(response)
            }
        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    suspend fun createPayment(sessionToken: String): Result<SessionPaymentData> {
        return try {
            val response = api.createPayment(sessionToken)
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error("Empty server response")
                val paymentData = body.data?.payment ?: return Result.Error("Missing payment data")
                Result.Success(paymentData)
            } else {
                handleErrorResponse(response)
            }
        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    fun observeSession(sessionId: String): Flow<PrintSession> {
        val channel = supabase.realtime.channel("session_$sessionId")
        return channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "print_sessions"
            filter = "id=eq.$sessionId"
        }.map { 
            it.decodeRecord<PrintSession>()
        }.onStart {
            try { channel.subscribe() } catch (_: Exception) {}
        }
    }

    /* ---------------- ERROR HANDLING ---------------- */

    private fun <T> handleErrorResponse(response: Response<*>): Result<T> {
        val errorBody = response.errorBody()?.string()
        return try {
            val apiResponse = Gson().fromJson(errorBody, ApiResponse::class.java)
            // If the server provides a specific message, use it
            if (!apiResponse.message.isNullOrBlank()) {
                Result.Error(apiResponse.message)
            } else {
                throw Exception("Fallback to status code")
            }
        } catch (_: Exception) {
            when (response.code()) {
                401 -> Result.Error("Session expired. Please login again.")
                403 -> Result.Error("You already have an active session at another shop. Please finish it first.")
                404 -> Result.Error("Session not found.")
                500, 502, 503 -> Result.Error("Server error. Please try again later.")
                else -> Result.Error("Action failed (Error ${response.code()})")
            }
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
