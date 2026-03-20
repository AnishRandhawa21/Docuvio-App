package com.docuvio.app.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    }

    /* ---------------- TOKEN ---------------- */

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    /**
     * ✅ SAFE for OkHttp Interceptor
     * Always returns the real token from DataStore
     */
    fun getTokenBlocking(): String? = runBlocking {
        context.dataStore.data.first()[TOKEN_KEY]
    }

    val tokenFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[TOKEN_KEY] }

    /* ---------------- USER INFO ---------------- */

    suspend fun saveUserInfo(userId: String, userName: String, email: String, role: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
            prefs[USER_NAME_KEY] = userName
            prefs[USER_EMAIL_KEY] = email
            prefs[USER_ROLE_KEY] = role
        }
    }

    suspend fun getUserName(): String? {
        return context.dataStore.data.first()[USER_NAME_KEY]
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
}
