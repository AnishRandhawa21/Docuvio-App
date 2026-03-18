package com.docuvio.app.data.api

import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterDeviceRequest(
    val userId: String,
    val token: String,
    val platform: String = "android"
)

interface NotificationApi {

    @POST("notifications/register-device")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    )
}