package com.docuvio.app.core.network

import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.data.api.AuthApi
import com.docuvio.app.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 🔄 AuthAuthenticator
 * Automatically handles 401 Unauthorized responses by attempting to refresh the token.
 * Industry Level: Handles concurrent requests and token invalidation.
 */
class AuthAuthenticator(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi
) : Authenticator {

    companion object {
        // 🔒 Global lock to prevent multiple simultaneous refresh calls across all instances
        private val refreshLock = Any()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // ❌ Prevent infinite retry loop (max 2 attempts per request)
        if (responseCount(response) >= 2) {
            android.util.Log.e("AUTH", "🛑 Max retry attempts reached. Admitting defeat.")
            return null
        }

        // 🔑 Get tokens as they were WHEN THIS REQUEST WAS MADE
        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
        val refreshToken = tokenManager.getRefreshTokenBlocking()

        if (refreshToken.isNullOrBlank()) {
            android.util.Log.e("AUTH", "❌ No refresh token available.")
            return null
        }

        // 🔒 Synchronized block to ensure only one thread performs the actual refresh
        synchronized(refreshLock) {
            val latestAccessToken = tokenManager.getTokenBlocking()

            // ⚡ Scenario A: Another thread already refreshed the token while we were waiting
            // We compare what this request used (requestToken) with what is now in TokenManager.
            if (!latestAccessToken.isNullOrBlank() && latestAccessToken != requestToken) {
                android.util.Log.d("AUTH", "⚡ Using already refreshed token for retry.")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestAccessToken")
                    .build()
            }

            // ⚡ Scenario B: We are the first thread to reach the lock. Perform the refresh.
            android.util.Log.d("AUTH", "🔄 Performing token refresh...")
            try {
                val refreshResponse = authApi.refreshToken(
                    RefreshTokenRequest(refreshToken)
                ).execute()

                if (refreshResponse.isSuccessful) {
                    val body = refreshResponse.body()
                    if (body != null) {
                        val newAccess = body.data.access_token
                        val newRefresh = body.data.refresh_token

                        // 💾 Save new tokens immediately
                        runBlocking {
                            tokenManager.saveToken(newAccess)
                            tokenManager.saveRefreshToken(newRefresh)
                        }
                        android.util.Log.d("AUTH", "✅ Token refreshed successfully.")

                        return response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccess")
                            .build()
                    }
                } else if (refreshResponse.code() == 401 || refreshResponse.code() == 403 || refreshResponse.code() == 400) {
                    // 🔥 Refresh token itself is rejected or expired
                    android.util.Log.e("AUTH", "🛑 Session killed by server (${refreshResponse.code()}).")
                    runBlocking { tokenManager.clearAll() }
                } else {
                    android.util.Log.e("AUTH", "❌ Refresh call failed with code: ${refreshResponse.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AUTH", "❌ Refresh exception", e)
            }

            return null // Give up, will trigger the 401 logout in ApiClient
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
