package com.docuvio.app.data.api

import com.docuvio.app.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface OrderApi {

    @GET("students/orders")
    suspend fun getOrders(): Response<OrdersResponse>

    @POST("students/orders")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<ApiResponse<CreateOrderResponse>>

    @POST("students/orders")
    suspend fun createWalkInOrder(
        @Body request: WalkInOrderRequest
    ): Response<ApiResponse<CreateOrderResponse>>


    @Multipart
    @POST("files/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @POST("students/orders/{orderId}/documents")
    suspend fun attachDocument(
        @Path("orderId") orderId: String,
        @Body request: AttachDocumentRequest
    ): Response<Unit>

    @POST("payments/create-order")
    suspend fun createPayment(
        @Body request: CreatePaymentRequest
    ): Response<ApiResponse<CreatePaymentResponse>>

    @POST("payments/verify")
    suspend fun verifyPayment(
        @Body request: VerifyPaymentRequest
    ): Response<VerifyPaymentResponse>

    @POST("students/orders/{orderId}/documents")
    suspend fun attachWalkInDocument(
        @Path("orderId") orderId: String,
        @Body request: AttachWalkInDocument
    ): Response<Unit>
}
