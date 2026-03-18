package com.docuvio.app.utils

fun isValidPassword(password: String): Boolean {
    return password.length >= 8
}
