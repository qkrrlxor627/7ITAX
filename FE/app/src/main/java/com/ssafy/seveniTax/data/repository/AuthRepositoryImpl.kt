package com.ssafy.seveniTax.data.repository

import com.ssafy.seveniTax.data.local.SecureStorage
import com.ssafy.seveniTax.data.model.auth.LoginRequest
import com.ssafy.seveniTax.data.model.auth.ReissueRequest
import com.ssafy.seveniTax.data.model.auth.SetupPinRequest
import com.ssafy.seveniTax.data.model.auth.TokenResponse
import com.ssafy.seveniTax.data.model.auth.VerifyIdentityRequest
import com.ssafy.seveniTax.data.model.auth.VerifyIdentityResponse
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.remote.AuthApi
import org.json.JSONObject
import retrofit2.Response
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val secureStorage: SecureStorage
) : AuthRepository {

    override suspend fun verifyIdentity(request: VerifyIdentityRequest): ApiResponse<VerifyIdentityResponse> {
        val response = authApi.verifyIdentity(request)
        return response.body() ?: throw Exception(extractErrorMessage(response, "본인인증 요청에 실패했습니다."))
    }

    override suspend fun setupPin(verifyToken: String, pin: String): ApiResponse<TokenResponse> {
        val response = authApi.setupPin(verifyToken, SetupPinRequest(pin))
        val body = response.body() ?: throw Exception(extractErrorMessage(response, "PIN 설정에 실패했습니다."))
        body.data?.let { saveTokens(it.accessToken, it.refreshToken) }
        return body
    }

    override suspend fun login(phoneNumber: String, pin: String): ApiResponse<TokenResponse> {
        val response = authApi.login(LoginRequest(phoneNumber, pin))
        val body = response.body() ?: throw Exception(extractErrorMessage(response, "로그인에 실패했습니다."))
        body.data?.let { saveTokens(it.accessToken, it.refreshToken) }
        return body
    }

    override suspend fun reissue(refreshToken: String): ApiResponse<TokenResponse> {
        val response = authApi.reissue(ReissueRequest(refreshToken))
        val body = response.body() ?: throw Exception(extractErrorMessage(response, "토큰 재발급에 실패했습니다."))
        body.data?.let { saveTokens(it.accessToken, it.refreshToken) }
        return body
    }

    override suspend fun logout() {
        try {
            val token = secureStorage.getAccessToken()
            if (token != null) {
                authApi.logout("Bearer $token")
            }
        } catch (_: Exception) {
        }
        clearSession()
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        secureStorage.saveAccessToken(accessToken)
        secureStorage.saveRefreshToken(refreshToken)
    }

    override fun saveUserInfo(userId: Long, phoneNumber: String, name: String) {
        secureStorage.saveUserId(userId.toString())
        secureStorage.savePhoneNumber(phoneNumber)
        secureStorage.saveUserName(name)
    }

    override fun getStoredPhoneNumber(): String? = secureStorage.getPhoneNumber()

    override fun hasStoredCredentials(): Boolean = secureStorage.isLoggedIn()

    override fun clearSession() {
        secureStorage.clearAll()
    }

    private fun extractErrorMessage(response: Response<*>, fallback: String): String {
        val raw = response.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return fallback
        // HTML 응답이면 fallback 메시지 반환
        if (raw.trimStart().startsWith("<")) return fallback
        return runCatching {
            JSONObject(raw).optString("message").takeIf { it.isNotBlank() } ?: fallback
        }.getOrDefault(fallback)
    }
}
