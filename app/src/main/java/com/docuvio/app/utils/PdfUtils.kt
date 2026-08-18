package com.docuvio.app.utils

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

object PdfUtils {

    sealed class PdfResult {
        data class Success(val pageCount: Int) : PdfResult()
        object PasswordProtected : PdfResult()
        data class Error(val message: String) : PdfResult()
    }

    fun getPdfPageCount(file: File): PdfResult {
        if (!file.exists()) return PdfResult.Error("File does not exist")
        if (file.length() == 0L) return PdfResult.Error("File is empty")
        
        var renderer: PdfRenderer? = null
        var descriptor: ParcelFileDescriptor? = null

        return try {
            descriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            if (descriptor != null) {
                renderer = PdfRenderer(descriptor)
                PdfResult.Success(renderer.pageCount)
            } else {
                PdfResult.Error("Could not open file descriptor")
            }
        } catch (e: SecurityException) {
            Log.e("PdfUtils", "PDF is password protected: ${file.name}")
            PdfResult.PasswordProtected
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown PDF error"
            Log.e("PdfUtils", "Failed to get page count for ${file.name}: $msg")
            
            if (msg.contains("password", ignoreCase = true)) {
                PdfResult.PasswordProtected
            } else {
                PdfResult.Error("Invalid or corrupted PDF file")
            }
        } finally {
            try {
                renderer?.close()
                descriptor?.close()
            } catch (_: Exception) {}
        }
    }
}
