package com.docuvio.app.data.model

import com.google.gson.annotations.SerializedName

// ---------- REQUESTS ----------

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class SignupRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String = "student",
    @SerializedName("organisation_id") val organisation_id: String
)


// ---------- RESPONSES ----------

data class LoginResponse(
    @SerializedName("data") val data: LoginData
)

data class LoginData(
    @SerializedName("user") val user: User,
    @SerializedName("session") val session: Session
)

data class Session(
    @SerializedName("access_token") val access_token: String,
    @SerializedName("refresh_token") val refresh_token: String
)

// ---------- USER ----------

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("user_metadata") val user_metadata: UserMetadata
)

data class UserMetadata(
    @SerializedName("role") val role: String,
    @SerializedName("name") val name: String? = null
)

data class OrganisationResponse(
    @SerializedName("data") val data: List<Organisation>
)

data class Organisation(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refresh_token: String
)

data class RefreshTokenResponse(
    @SerializedName("data") val data: SessionData
)

data class SessionData(
    @SerializedName("access_token") val access_token: String,
    @SerializedName("refresh_token") val refresh_token: String
)
