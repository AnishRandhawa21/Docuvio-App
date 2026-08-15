package com.docuvio.app.data.repository

import android.util.Log
import com.docuvio.app.data.api.ShopApi
import com.docuvio.app.data.model.PrintOptions
import com.docuvio.app.data.model.Shop
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ShopRepository(
    private val shopApi: ShopApi,
    private val supabase: SupabaseClient
) {

    /* ---------------- GET SHOPS ---------------- */

    suspend fun getShops(): Result<List<Shop>> {
        return try {
            val response = shopApi.getShops()

            if (!response.isSuccessful) {
                return mapErrorResponse(response.code())
            }

            val body = response.body()
                ?: return Result.Error("Empty server response")

            val data = body.data ?: return Result.Error("Data missing from server")
            Result.Success(data)

        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    /* ---------------- REALTIME ---------------- */

    fun observeShops(): Flow<Shop> {
        val channel = supabase.realtime.channel("shops_realtime_final")
        
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "shops"
        }.mapNotNull { action ->
            try {
                when (action) {
                    is PostgresAction.Update -> action.decodeRecord<Shop>()
                    is PostgresAction.Insert -> action.decodeRecord<Shop>()
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }.onStart {
            try {
                supabase.realtime.connect()
                channel.subscribe()
            } catch (_: Exception) { }
        }
    }

    /* ---------------- PRINT OPTIONS ---------------- */

    suspend fun getPrintOptions(shopId: String): Result<PrintOptions> {
        return try {
            val response = shopApi.getPrintOptions(shopId)

            if (!response.isSuccessful) {
                return mapErrorResponse(response.code())
            }

            val body = response.body()
                ?: return Result.Error("Empty server response")

            val data = body.data ?: return Result.Error("Data missing from server")
            Result.Success(data)

        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    /* ---------------- SINGLE SHOP ---------------- */

    suspend fun getShop(shopId: String): Result<Shop> {
        return try {
            val response = shopApi.getShop(shopId)

            if (!response.isSuccessful) {
                return mapErrorResponse(response.code())
            }

            val body = response.body()
                ?: return Result.Error("Empty server response")

            val data = body.data ?: return Result.Error("Data missing from server")
            Result.Success(data)

        } catch (e: Exception) {
            Result.Error(mapNetworkError(e))
        }
    }

    /* ---------------- ERROR MAPPING ---------------- */

    private fun <T> mapErrorResponse(code: Int): Result<T> {
        return when (code) {
            401, 403 -> Result.Error("Session expired. Please login again.")
            404      -> Result.Error("Resource not found.")
            500, 502, 503 -> Result.Error("Server error. Please try again later.")
            else     -> Result.Error("Something went wrong (Error $code)")
        }
    }

    private fun mapNetworkError(e: Exception): String {
        return when (e) {
            is UnknownHostException  -> "No internet connection."
            is SocketTimeoutException -> "Connection timed out. Please try again."
            is IOException           -> "Network error. Please check your connection."
            else                     -> "Something went wrong. Please try again."
        }
    }
}
