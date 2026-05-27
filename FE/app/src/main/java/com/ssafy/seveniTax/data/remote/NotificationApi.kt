package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.common.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface NotificationApi {

    @POST("fcm/token")
    suspend fun registerFcmToken(
        @Body body: FcmTokenRequest
    ): Response<ApiResponse<Unit>>

    @DELETE("fcm/token")
    suspend fun deleteFcmToken(
        @Body body: FcmTokenRequest
    ): Response<ApiResponse<Unit>>
}

data class FcmTokenRequest(
    val token: String
)
