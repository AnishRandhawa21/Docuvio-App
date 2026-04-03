package com.docuvio.app.data.repository

import android.util.Log
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.data.api.AuthApi
import com.docuvio.app.data.api.UnregisterDeviceRequest
import com.docuvio.app.data.model.LoginRequest
import com.docuvio.app.data.model.LoginResponse
import com.docuvio.app.data.model.Organisation
import com.docuvio.app.data.model.SignupRequest
import kotlinx.coroutines.tasks.await
import com.docuvio.app.data.api.NotificationApi
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val notificationApi: NotificationApi
) {

    /* ---------------- LOGIN ---------------- */

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = authApi.login(LoginRequest(email, password))

            if (!response.isSuccessful) {
                Log.e("AUTH", "Error code: ${response.code()}")
                Log.e("AUTH", "Error body: ${response.errorBody()?.string()}")
                Log.e("API_ERROR", "Code: ${response.code()}")
                Log.e("API_ERROR", "Body: ${response.errorBody()?.string()}")
                return when (response.code()) {
                    401, 403 -> Result.Error("Invalid email or password")
                    500 -> Result.Error("Server error. Please try again later.")
                    else -> Result.Error("Login failed. Please try again.")
                }
            }

            val body = response.body()
                ?: return Result.Error("Empty server response")

            val accessToken = body.data.session.access_token
            val refreshToken = body.data.session.refresh_token

            tokenManager.saveToken(accessToken)
            tokenManager.saveRefreshToken(refreshToken)

            tokenManager.saveUserInfo(
                userId = body.data.user.id,
                userName = body.data.user.user_metadata.name
                    ?: email.substringBefore("@"),
                email = email,
                role = body.data.user.user_metadata.role
            )

            Result.Success(body)

        } catch (e: Exception) {

            val message = when {
                e.message?.contains("Unable to resolve host", true) == true ->
                    "Cannot connect to server. Check your internet connection."

                e.message?.contains("timeout", true) == true ->
                    "Server is taking too long to respond."

                else ->
                    "Something went wrong. Please try again."
            }

            Result.Error(message)
        }
    }

    /* ---------------- SIGNUP ---------------- */

    suspend fun signup(
        name: String,
        email: String,
        password: String,
        organisationId: String
    ): Result<Unit> {
        return try {
            val response = authApi.signup(
                SignupRequest(
                    name = name,
                    email = email,
                    password = password,
                    organisation_id = organisationId
                )
            )

            if (!response.isSuccessful) {
                return when (response.code()) {
                    409 -> Result.Error("Account already exists with this email.")
                    400 -> Result.Error("Invalid signup data.")
                    500 -> Result.Error("Server error. Please try again later.")
                    else -> Result.Error("Signup failed. Please try again.")
                }
            }

            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e("AUTH_ERROR", "Login exception", e)

            val message = when {
                e.message?.contains("Unable to resolve host", true) == true ->
                    "Cannot connect to server. Check your internet connection."

                e.message?.contains("timeout", true) == true ->
                    "Server is taking too long to respond."

                else ->
                    "Something went wrong. Please try again."
            }

            Result.Error(message)
        }
    }

    /* ---------------- Organisation---------------- */
    suspend fun getOrganisations(): Result<List<Organisation>> {
        return try {
            val response = authApi.getOrganisations()

            Log.d("ORG", "Response code: ${response.code()}")
            Log.d("ORG", "Body: ${response.body()}")

            if (!response.isSuccessful) {
                Log.e("ORG_ERROR", "Error body: ${response.errorBody()?.string()}")
                return Result.Error("Failed to load organisations.")
            }

            val body = response.body()

            if (body?.data == null) {
                Log.e("ORG_ERROR", "Body data is null")
                return Result.Error("Invalid server response")
            }

            Result.Success(body.data)

        } catch (e: Exception) {
            Log.e("ORG_EXCEPTION", "Exception", e)
            Result.Error("Something went wrong. Please try again.")
        }
    }



    /* ---------------- LOGOUT ---------------- */

    suspend fun logout() {

        try {

            val userId = tokenManager.getUserIdBlocking()

            val token = com.google.firebase.messaging.FirebaseMessaging
                .getInstance()
                .token
                .await()

            if (userId != null) {

                notificationApi.unregisterDevice(
                    UnregisterDeviceRequest(
                        userId = userId,
                        token = token
                    )
                )
            }

            com.google.firebase.messaging.FirebaseMessaging
                .getInstance()
                .deleteToken()

        } catch (e: Exception) {
            Log.e("FCM", "Logout cleanup failed", e)
        }

        tokenManager.clearAll()
    }
}
