package com.docuvio.app.data.api

import com.docuvio.app.data.model.*
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface PrintSessionApi {

    @POST("print-sessions/shop/{publicCode}/start")
    suspend fun startSession(
        @Path("publicCode") publicCode: String
    ): Response<ApiResponse<StartSessionResponse>>

    @POST("print-sessions/session/{sessionToken}/connect")
    suspend fun connectSession(
        @Path("sessionToken") sessionToken: String
    ): Response<ApiResponse<Any?>>

    @GET("print-sessions/session/{sessionToken}")
    suspend fun getSession(
        @Path("sessionToken") sessionToken: String
    ): Response<ApiResponse<PrintSession>>

    @PATCH("print-sessions/session/{sessionToken}/customer")
    suspend fun submitCustomerDetails(
        @Path("sessionToken") sessionToken: String,
        @Body request: CustomerDetailsRequest
    ): Response<ApiResponse<Any?>>

    @POST("print-sessions/session/{sessionToken}/files/start")
    suspend fun startFileUploadPhase(
        @Path("sessionToken") sessionToken: String
    ): Response<ApiResponse<PrintSession>>

    @POST("print-sessions/session/{sessionToken}/files/start")
    suspend fun startFileUpload(
        @Path("sessionToken") sessionToken: String,
        @Body request: StartUploadRequest
    ): Response<ApiResponse<PrintSession>>

    @Multipart
    @POST("print-sessions/session/{sessionToken}/files")
    suspend fun uploadFile(
        @Path("sessionToken") sessionToken: String,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<PrintSessionFile>>

    @GET("print-sessions/session/{sessionToken}/files")
    suspend fun getSessionFiles(
        @Path("sessionToken") sessionToken: String
    ): Response<ApiResponse<List<PrintSessionFile>>>

    @GET("print-sessions/files/{fileId}/url")
    suspend fun getFileUrl(
        @Path("fileId") fileId: String
    ): Response<ApiResponse<FileUrlResponse>>

    @GET("print-sessions/session/{sessionToken}/quote")
    suspend fun getQuote(
        @Path("sessionToken") sessionToken: String
    ): Response<ApiResponse<PrintSessionQuote>>

    @POST("print-sessions/session/{sessionToken}/payment/create")
    suspend fun createPayment(
        @Path("sessionToken") sessionToken: String
    ): Response<ApiResponse<SessionPaymentResponse>>
}

data class StartUploadRequest(
    @SerializedName("file_name") val fileName: String,
    @SerializedName("mime_type") val mimeType: String
)

data class FileUrlResponse(
    @SerializedName("url") val url: String
)
