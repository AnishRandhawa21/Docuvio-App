package com.docuvio.app.utils

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

object PdfUtils {

    fun getPdfPageCount(file: File): Int {
        if (!file.exists() || file.length() == 0L) return 0
        
        var renderer: PdfRenderer? = null
        var descriptor: ParcelFileDescriptor? = null

        return try {
            descriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            if (descriptor != null) {
                renderer = PdfRenderer(descriptor)
                renderer.pageCount
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e("PdfUtils", "Failed to get page count for ${file.name}: ${e.message}")
            0
        } finally {
            try {
                renderer?.close()
                descriptor?.close()
            } catch (e: Exception) {
                // Ignore closing errors
            }
        }
    }
}
