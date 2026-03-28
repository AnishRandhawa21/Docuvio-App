package com.docuvio.app.utils

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

object PdfUtils {

    fun getPdfPageCount(file: File): Int {
        var renderer: PdfRenderer? = null
        var descriptor: ParcelFileDescriptor? = null

        return try {
            descriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            renderer = PdfRenderer(descriptor)
            renderer.pageCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            renderer?.close()
            descriptor?.close()
        }
    }
}
