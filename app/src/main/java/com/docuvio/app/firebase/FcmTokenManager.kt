package com.docuvio.app.firebase

object FcmTokenManager {

    private var token: String? = null

    fun save(newToken: String) {
        token = newToken
    }

    fun get(): String? = token

    fun clear() {
        token = null
    }
}
