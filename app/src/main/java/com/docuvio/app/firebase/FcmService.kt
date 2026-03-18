package com.docuvio.app.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token received → $token")
        FcmTokenManager.save(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM", "Message received")

        val title =
            message.notification?.title
                ?: message.data["title"]
                ?: "Docuvio"

        val body =
            message.notification?.body
                ?: message.data["body"]
                ?: ""

        NotificationHelper.showNotification(
            context = this,
            title = title,
            body = body,
            data = message.data
        )
    }
}