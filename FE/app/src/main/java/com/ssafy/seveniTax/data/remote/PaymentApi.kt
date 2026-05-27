package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.payment.*
import retrofit2.Response
import retrofit2.http.*

interface PaymentApi {

    @POST("payments/qr/token")
    suspend fun createQrToken(@Body body: QrTokenCreateRequest): Response<ApiResponse<QrTokenCreateResponse>>

    @GET("payments/qr/token/{token}")
    suspend fun getQrPaymentInfo(@Path("token") token: String): Response<ApiResponse<QrPaymentInfoResponse>>

    @POST("payments/qr/token/{token}/confirm")
    suspend fun confirmQrPayment(@Path("token") token: String): Response<ApiResponse<QrPaymentResponse>>

    @GET("payments/qr/token/{token}/status")
    suspend fun getQrPaymentStatus(@Path("token") token: String): Response<ApiResponse<QrPaymentStatusResponse>>

    @GET("cards/merchants")
    suspend fun getMerchants(): Response<ApiResponse<List<MerchantResponse>>>
}
