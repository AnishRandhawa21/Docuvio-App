package com.docuvio.app.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast

fun openEmailApp(context: Context) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_APP_EMAIL)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "No email app found",
            Toast.LENGTH_SHORT
        ).show()
    }
}
