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
 */
class AuthAuthenticator(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // ❌ Prevent infinite retry loop (max 2 attempts per request)
        if (responseCount(response) >= 2) return null

        // 🔑 Get current tokens
        val currentAccessToken = tokenManager.getTokenBlocking()
        val refreshToken = tokenManager.getRefreshTokenBlocking()
            ?: return null

        // 🔒 Synchronized block to prevent multiple simultaneous refresh calls
        synchronized(this) {
            val latestAccessToken = tokenManager.getTokenBlocking()

            // ⚡ Check if the token was already refreshed by another concurrent request
            val tokenToUse = if (latestAccessToken != currentAccessToken && !latestAccessToken.isNullOrBlank()) {
                latestAccessToken
            } else {
                // 🔄 Perform actual refresh call (Synchronous call required by Authenticator)
                try {
                    val refreshResponse = authApi.refreshToken(
                        RefreshTokenRequest(refreshToken)
                    ).execute()

                    if (refreshResponse.isSuccessful) {
                        val body = refreshResponse.body()
                        if (body != null) {
                            val newAccess = body.data.access_token
                            val newRefresh = body.data.refresh_token

                            // 💾 Save new tokens
                            runBlocking {
                                tokenManager.saveToken(newAccess)
                                tokenManager.saveRefreshToken(newRefresh)
                            }
                            android.util.Log.d("AUTH", "🔄 Token refreshed successfully")
                            newAccess
                        } else null
                    } else {
                        android.util.Log.e("AUTH", "❌ Refresh failed: ${refreshResponse.code()}")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AUTH", "❌ Refresh exception", e)
                    null
                }
            }

            // 🔁 If we have a new/latest token, retry the original request
            return if (!tokenToUse.isNullOrBlank()) {
                response.request.newBuilder()
                    .header("Authorization", "Bearer $tokenToUse")
                    .build()
            } else {
                null // Give up, will trigger the 401 logout in ApiClient
            }
        }
    }

    /**
     * Helper to track how many times this specific request has been retried.
     */
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
