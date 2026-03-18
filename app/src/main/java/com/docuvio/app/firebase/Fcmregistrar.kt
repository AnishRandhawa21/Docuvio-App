package com.docuvio.app.firebase

import android.util.Log
import com.docuvio.app.data.api.NotificationApi
import com.docuvio.app.data.api.RegisterDeviceRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

suspend fun registerFcmToken(
    userId: String,
    notificationApi: NotificationApi
) {
    try {
        val token = FirebaseMessaging.getInstance().token.await()

        notificationApi.registerDevice(
            RegisterDeviceRequest(
                userId = userId,
                token = token,
                platform = "android"
            )
        )

        Log.d("FCM", "Token registered successfully")

    } catch (e: Exception) {
        Log.e("FCM", "Token registration failed", e)
    }
}