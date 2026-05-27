package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.model.auth.TokenResponse
import com.ssafy.seveniTax.data.model.auth.VerifyIdentityRequest
import com.ssafy.seveniTax.data.model.auth.VerifyIdentityResponse
import com.ssafy.seveniTax.data.model.common.ApiResponse

interface AuthRepository {
    suspend fun verifyIdentity(request: VerifyIdentityRequest): ApiResponse<VerifyIdentityResponse>
    suspend fun setupPin(verifyToken: String, pin: String): ApiResponse<TokenResponse>
    suspend fun login(phoneNumber: String, pin: String): ApiResponse<TokenResponse>
    suspend fun reissue(refreshToken: String): ApiResponse<TokenResponse>
    suspend fun logout()

    fun saveTokens(accessToken: String, refreshToken: String)
    fun saveUserInfo(userId: Long, phoneNumber: String, name: String)
    fun getStoredPhoneNumber(): String?
    fun hasStoredCredentials(): Boolean
    fun clearSession()
}
