package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.data.model.auth.LoginRequest
import com.ssafy.seveniTax.data.model.auth.ReissueRequest
import com.ssafy.seveniTax.data.model.auth.SetupPinRequest
import com.ssafy.seveniTax.data.model.auth.TokenResponse
import com.ssafy.seveniTax.data.model.auth.VerifyIdentityRequest
import com.ssafy.seveniTax.data.model.auth.VerifyIdentityResponse
import com.ssafy.seveniTax.data.model.common.ApiResponse
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {

    @POST("auth/verify-identity")
    suspend fun verifyIdentity(
        @Body body: VerifyIdentityRequest
    ): Response<ApiResponse<VerifyIdentityResponse>>

    @POST("auth/setup-pin")
    suspend fun setupPin(
        @Header("X-Verify-Token") verifyToken: String,
        @Body body: SetupPinRequest
    ): Response<ApiResponse<TokenResponse>>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/reissue")
    suspend fun reissue(@Body body: ReissueRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Response<ApiResponse<Unit>>
}
