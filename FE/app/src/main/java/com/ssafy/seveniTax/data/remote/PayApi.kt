package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.model.pay.*
import retrofit2.Response
import retrofit2.http.*

interface PayApi {

    @POST("banking/accounts")
    suspend fun createAccount(@Body body: AccountCreateRequest): Response<ApiResponse<AccountResponse>>

    @GET("banking/accounts")
    suspend fun getAccounts(
        @Query("account_type") type: String? = null
    ): Response<ApiResponse<List<AccountResponse>>>

    @GET("banking/accounts/{id}")
    suspend fun getAccount(@Path("id") id: String): Response<ApiResponse<AccountResponse>>

    @GET("banking/accounts/{id}/balance")
    suspend fun getBalance(@Path("id") id: String): Response<ApiResponse<BalanceResponse>>

    @POST("pay/pin")
    suspend fun setPayPin(@Body body: PayPinRequest): Response<ApiResponse<Unit>>

    @POST("pay/pin/verify")
    suspend fun verifyPayPin(@Body body: PayPinRequest): Response<ApiResponse<Unit>>

    @GET("pay/pin/status")
    suspend fun getPayPinStatus(): Response<ApiResponse<PayPinStatusResponse>>
}
