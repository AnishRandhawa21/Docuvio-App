package com.docuvio.app.ui.order.utils


import android.util.Log
import com.docuvio.app.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object DocxConverter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun convertToPdf(file: File): File {
        Log.d("DocxConverter", "convertToPdf called — file: ${file.absolutePath}")
        Log.d("DocxConverter", "File exists: ${file.exists()}, size: ${file.length()} bytes")
        Log.d("DocxConverter", "CONVERTER_URL: ${BuildConfig.CONVERTER_URL}")

        if (!file.exists()) throw Exception("Source file not found: ${file.name}")
        if (file.length() == 0L) throw Exception("Source file is empty: ${file.name}")

        // ✅ Use the real DOCX MIME type so the server can identify the file
        val docxMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,                                          // filename with .docx extension
                file.asRequestBody(docxMimeType.toMediaType())     // ← correct MIME, not octet-stream
            )
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.CONVERTER_URL}/convert")
            .addHeader("x-api-key", BuildConfig.CONVERTER_API_KEY)
            .post(requestBody)
            .build()

        Log.d("DocxConverter", "Sending request to: ${request.url}")
        Log.d("DocxConverter", "File Content-Type in multipart: $docxMimeType")
        Log.d("DocxConverter", "Filename sent: ${file.name}")

        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            Log.e("DocxConverter", "Network call failed", e)
            throw Exception("Network error during conversion: ${e.message}")
        }

        Log.d("DocxConverter", "Response code: ${response.code}")

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "no body"
            Log.e("DocxConverter", "Server error body: $errorBody")
            throw Exception("Conversion failed (HTTP ${response.code}): $errorBody")
        }

        val pdfFile = File(
            file.parent,
            file.name.substringBeforeLast(".") + "_converted.pdf"
        )

        val bytes = response.body?.bytes()
            ?: throw Exception("Converter returned empty response body")

        Log.d("DocxConverter", "Received ${bytes.size} bytes from converter")

        if (bytes.isEmpty()) throw Exception("Converter returned zero bytes")

        pdfFile.writeBytes(bytes)
        Log.d("DocxConverter", "PDF written to: ${pdfFile.absolutePath}, size: ${pdfFile.length()}")

        return pdfFile
    }
}