package com.docuvio.app.data.api

import com.docuvio.app.data.model.ApiResponse
import com.docuvio.app.data.model.PrintOptions
import com.docuvio.app.data.model.Shop
import com.docuvio.app.data.model.ShopListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ShopApi {

    // GET /api/shops
    @GET("shops")
    suspend fun getShops(): Response<ShopListResponse>

    // GET /api/students/shops/:id/options
    @GET("students/shops/{shopId}/options")
    suspend fun getPrintOptions(
        @Path("shopId") shopId: String
    ): Response<ApiResponse<PrintOptions>>

    // ✅ ADD THIS
    @GET("shops/{shopId}")
    suspend fun getShop(
        @Path("shopId") shopId: String
    ): Response<ApiResponse<Shop>>
}