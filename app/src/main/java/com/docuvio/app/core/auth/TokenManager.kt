package com.docuvio.app.core.auth

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * 🔒 TokenManager
 * Handles secure storage and retrieval of authentication tokens and session data.
 * Uses Jetpack DataStore for persistence.
 */
class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_PHONE_KEY = stringPreferencesKey("user_phone")

        private val TOKEN_EXPIRY_KEY = stringPreferencesKey("token_expiry") 
        private val GUEST_SESSION_TOKEN_KEY = stringPreferencesKey("guest_session_token")
        private val GUEST_SESSION_DISMISSED_KEY = stringPreferencesKey("guest_session_dismissed_token")
        private val GUEST_NAME_KEY = stringPreferencesKey("guest_name")
        private val GUEST_PHONE_KEY = stringPreferencesKey("guest_phone")

        private val SAVED_EMAIL_KEY = stringPreferencesKey("saved_email")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val HAS_ACCEPTED_TERMS_KEY = booleanPreferencesKey("has_accepted_terms")
        private val PENDING_ORDER_ID_KEY = stringPreferencesKey("pending_order_id")
        private val PENDING_ORDER_TYPE_KEY = stringPreferencesKey("pending_order_type")
    }

    /* ---------------- SAVED CREDENTIALS ---------------- */

    suspend fun saveEmail(email: String) {
        context.dataStore.edit { it[SAVED_EMAIL_KEY] = email }
    }

    fun getSavedEmailBlocking(): String? = runBlocking {
        context.dataStore.data.first()[SAVED_EMAIL_KEY]
    }

    /* ---------------- USER PROFILE ---------------- */

    suspend fun savePhone(phone: String) {
        context.dataStore.edit { it[USER_PHONE_KEY] = phone }
    }

    fun getPhoneBlocking(): String? = runBlocking {
        context.dataStore.data.first()[USER_PHONE_KEY]
    }

    suspend fun saveGuestProfile(name: String, phone: String) {
        context.dataStore.edit { prefs ->
            prefs[GUEST_NAME_KEY] = name
            prefs[GUEST_PHONE_KEY] = phone
        }
    }

    fun getGuestNameBlocking(): String? = runBlocking {
        context.dataStore.data.first()[GUEST_NAME_KEY]
    }

    fun getGuestPhoneBlocking(): String? = runBlocking {
        context.dataStore.data.first()[GUEST_PHONE_KEY]
    }

    /* ---------------- TOKEN ---------------- */

    suspend fun saveGuestSessionToken(token: String?) {
        context.dataStore.edit { prefs ->
            if (token == null) {
                prefs.remove(GUEST_SESSION_TOKEN_KEY)
                prefs.remove(GUEST_SESSION_DISMISSED_KEY)
            } else {
                prefs[GUEST_SESSION_TOKEN_KEY] = token
            }
        }
    }

    suspend fun dismissGuestSession(token: String) {
        context.dataStore.edit { it[GUEST_SESSION_DISMISSED_KEY] = token }
    }

    val guestSessionTokenFlow: Flow<String?> =
        context.dataStore.data.map { prefs ->
            val token = prefs[GUEST_SESSION_TOKEN_KEY]
            val dismissedToken = prefs[GUEST_SESSION_DISMISSED_KEY]
            if (token != null && token == dismissedToken) null else token
        }

    val rawGuestSessionTokenFlow: Flow<String?> =
        context.dataStore.data.map { it[GUEST_SESSION_TOKEN_KEY] }

    val isUserLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[TOKEN_KEY].isNullOrBlank() || !prefs[REFRESH_TOKEN_KEY].isNullOrBlank()
    }

    fun getGuestSessionTokenBlocking(): String? = runBlocking {
        context.dataStore.data.first()[GUEST_SESSION_TOKEN_KEY]
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun saveTokenExpiry(expiry: Long) {
        context.dataStore.edit { it[TOKEN_EXPIRY_KEY] = expiry.toString() }
    }

    /**
     * ✅ isSessionValid
     * We consider a session valid if we have a refresh token (background fixing) 
     * OR a non-expired access token.
     */
    fun isSessionValid(): Boolean {
        val token = getTokenBlocking()
        val refreshToken = getRefreshTokenBlocking()
        val guestToken = getGuestSessionTokenBlocking()

        // If no tokens at all -> definitely logged out
        if (token.isNullOrBlank() && refreshToken.isNullOrBlank() && guestToken.isNullOrBlank()) return false

        // If we have a refresh token or guest token -> assume valid for startup
        if (!refreshToken.isNullOrBlank() || !guestToken.isNullOrBlank()) return true

        // Fallback: check JWT expiry if ONLY access token exists
        val jwtExpiry = if (!token.isNullOrBlank()) getJwtExpiry(token) else null
        return if (jwtExpiry != null) System.currentTimeMillis() < jwtExpiry else false
    }

    private fun getJwtExpiry(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            val expSeconds = json.optLong("exp", 0)
            if (expSeconds == 0L) null else expSeconds * 1000 
        } catch (_: Exception) { null }
    }

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

    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[USER_NAME_KEY] }
    val userEmailFlow: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL_KEY] }

    fun getUserNameBlocking(): String? = runBlocking {
        context.dataStore.data.first()[USER_NAME_KEY]
    }

    /* ---------------- CLEAR (SAFE) ---------------- */

    /**
     * Wipes Auth data but keeps Guest Session and Terms status.
     */
    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_ROLE_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_PHONE_KEY)
            prefs.remove(TOKEN_EXPIRY_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
        }
    }

    fun getUserIdBlocking(): String? = runBlocking {
        context.dataStore.data.first()[USER_ID_KEY]
    }

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { it[REFRESH_TOKEN_KEY] = token }
    }

    fun getRefreshTokenBlocking(): String? = runBlocking {
        context.dataStore.data.first()[REFRESH_TOKEN_KEY]
    }

    /* ---------------- TERMS ACCEPTANCE ---------------- */

    suspend fun saveTermsAccepted(accepted: Boolean) {
        context.dataStore.edit { it[HAS_ACCEPTED_TERMS_KEY] = accepted }
    }

    fun hasAcceptedTermsFlow(): Flow<Boolean> =
        context.dataStore.data.map { it[HAS_ACCEPTED_TERMS_KEY] ?: false }

    fun hasAcceptedTermsBlocking(): Boolean = runBlocking {
        context.dataStore.data.first()[HAS_ACCEPTED_TERMS_KEY] ?: false
    }

    /* ---------------- PENDING ORDER ---------------- */

    suspend fun savePendingOrderId(orderId: String?, type: String? = null) {
        context.dataStore.edit { prefs ->
            if (orderId == null) {
                prefs.remove(PENDING_ORDER_ID_KEY)
                prefs.remove(PENDING_ORDER_TYPE_KEY)
            } else {
                prefs[PENDING_ORDER_ID_KEY] = orderId
                if (type != null) prefs[PENDING_ORDER_TYPE_KEY] = type
            }
        }
    }

    fun getPendingOrderIdBlocking(): String? = runBlocking {
        context.dataStore.data.first()[PENDING_ORDER_ID_KEY]
    }

    fun getPendingOrderTypeBlocking(): String? = runBlocking {
        context.dataStore.data.first()[PENDING_ORDER_TYPE_KEY]
    }
}
