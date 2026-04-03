package com.docuvio.app.core.auth

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")

        private val TOKEN_EXPIRY_KEY = stringPreferencesKey("token_expiry") // fallback
    }

    /* ---------------- TOKEN ---------------- */

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    suspend fun saveTokenExpiry(expiry: Long) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_EXPIRY_KEY] = expiry.toString()
        }
    }

    /**
     * ✅ MAIN SESSION CHECK (JWT आधारित)
     */
    fun isSessionValid(): Boolean {
        val token = getTokenBlocking() ?: return false

        val jwtExpiry = getJwtExpiry(token)

        // If JWT expiry found → use it
        if (jwtExpiry != null) {
            return System.currentTimeMillis() < jwtExpiry
        }

        // Fallback to stored expiry
        val fallbackExpiry = getTokenExpiryBlocking() ?: return false
        return System.currentTimeMillis() < fallbackExpiry
    }

    /**
     * 🔥 Extract expiry from JWT (exp claim)
     */
    private fun getJwtExpiry(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)

            val expSeconds = json.optLong("exp", 0)
            if (expSeconds == 0L) return null

            expSeconds * 1000 // convert to milliseconds
        } catch (_: Exception) {
            null
        }
    }

    /**
     * ✅ SAFE for OkHttp Interceptor
     */
    fun getTokenBlocking(): String? = runBlocking {
        context.dataStore.data.first()[TOKEN_KEY]
    }

    /* ---------------- USER INFO ---------------- */

    suspend fun saveUserInfo(userId: String, userName: String, email: String, role: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
            prefs[USER_NAME_KEY] = userName
            prefs[USER_EMAIL_KEY] = email
            prefs[USER_ROLE_KEY] = role
        }
    }

    val userNameFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[USER_NAME_KEY] }

    val userEmailFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[USER_EMAIL_KEY] }

    /* ---------------- CLEAR ---------------- */

    suspend fun clearAll() {
        context.dataStore.edit { prefs -> prefs.clear() }
    }

    fun getUserIdBlocking(): String? = runBlocking {
        context.dataStore.data.first()[USER_ID_KEY]
    }

    fun getTokenExpiryBlocking(): Long? = runBlocking {
        context.dataStore.data.first()[TOKEN_EXPIRY_KEY]?.toLongOrNull()
    }
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit {
            it[REFRESH_TOKEN_KEY] = token
        }
    }

    fun getRefreshTokenBlocking(): String? = runBlocking {
        context.dataStore.data.first()[REFRESH_TOKEN_KEY]
    }
}