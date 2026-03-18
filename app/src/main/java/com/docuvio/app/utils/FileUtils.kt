package com.docuvio.app.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object FileUtils {
    fun getFileName(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                return it.getString(nameIndex)
            }
        }
        return "document.pdf"
    }
}
