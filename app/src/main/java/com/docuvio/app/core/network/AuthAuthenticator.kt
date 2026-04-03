package com.docuvio.app.core.network

import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.data.api.AuthApi
import com.docuvio.app.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class AuthAuthenticator(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {

        // ❌ Prevent infinite retry loop
        if (responseCount(response) >= 2) return null

        // 🔑 Get refresh token
        val refreshToken = tokenManager.getRefreshTokenBlocking()
            ?: return null

        return try {
            // 🔄 Call refresh API (SYNC call required)
            val refreshResponse = authApi.refreshToken(
                RefreshTokenRequest(refreshToken)
            ).execute()
            println("🔄 REFRESH TOKEN CALLED")

            // ❌ If refresh fails → logout flow
            if (!refreshResponse.isSuccessful) return null

            val body = refreshResponse.body() ?: return null

            val newAccess = body.data.access_token
            val newRefresh = body.data.refresh_token

            // 💾 Save new tokens
            runBlocking {
                tokenManager.saveToken(newAccess)
                tokenManager.saveRefreshToken(newRefresh)
            }

            // 🔁 Retry original request with new token
            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()

        } catch (e: Exception) {
            null
        }
    }

    /**
     * 🔁 Prevent infinite retry loop
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