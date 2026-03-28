package com.docuvio.app.ui.order.utils

import android.util.Log
import com.docuvio.app.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DocxConverter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun convertToPdf(file: File): File = suspendCancellableCoroutine { continuation ->
        Log.d("DocxConverter", "convertToPdf called — file: ${file.absolutePath}")
        
        if (!file.exists()) {
            continuation.resumeWithException(Exception("Source file not found: ${file.name}"))
            return@suspendCancellableCoroutine
        }
        if (file.length() == 0L) {
            continuation.resumeWithException(Exception("Source file is empty: ${file.name}"))
            return@suspendCancellableCoroutine
        }

        val docxMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(docxMimeType.toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.CONVERTER_URL}/convert")
            .addHeader("x-api-key", BuildConfig.CONVERTER_API_KEY)
            .post(requestBody)
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DocxConverter", "Network call failed", e)
                continuation.resumeWithException(Exception("Network error during conversion: ${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        val errorBody = resp.body?.string() ?: "no body"
                        Log.e("DocxConverter", "Server error body: $errorBody")
                        continuation.resumeWithException(Exception("Conversion failed (HTTP ${resp.code})"))
                        return
                    }

                    try {
                        val pdfFile = File(
                            file.parent,
                            file.name.substringBeforeLast(".") + "_converted.pdf"
                        )

                        val bytes = resp.body?.bytes()
                            ?: throw Exception("Converter returned empty response body")

                        if (bytes.isEmpty()) throw Exception("Converter returned zero bytes")

                        pdfFile.writeBytes(bytes)
                        Log.d("DocxConverter", "PDF written to: ${pdfFile.absolutePath}, size: ${pdfFile.length()}")
                        continuation.resume(pdfFile)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
    }
}
