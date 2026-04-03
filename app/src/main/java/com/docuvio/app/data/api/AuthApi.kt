package com.docuvio.app.data.api

import com.docuvio.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun signup(@Body request: SignupRequest): Response<Unit>

    @GET("auth/organisations")
    suspend fun getOrganisations(): Response<OrganisationResponse>

    @POST("auth/refresh")
    fun refreshToken(
        @Body request: RefreshTokenRequest
    ): retrofit2.Call<RefreshTokenResponse>
}