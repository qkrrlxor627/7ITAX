package com.ssafy.seveniTax.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.ssafy.seveniTax.data.local.SecureStorage
import com.ssafy.seveniTax.data.model.auth.ConsentRequest
import com.ssafy.seveniTax.data.model.auth.LoginRequest
import com.ssafy.seveniTax.data.model.auth.ReissueRequest
import com.ssafy.seveniTax.data.model.auth.SetupPinRequest
import com.ssafy.seveniTax.data.model.auth.TokenResponse
import com.ssafy.seveniTax.data.model.auth.VerifyIdentityRequest
import com.ssafy.seveniTax.data.model.auth.VerifyIdentityResponse
import com.ssafy.seveniTax.data.model.common.ApiResponse
import com.ssafy.seveniTax.data.remote.AuthApi
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import retrofit2.Response
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val secureStorage: SecureStorage,
    @param:ApplicationContext private val context: Context
) : AuthRepository {

    override suspend fun verifyIdentity(request: VerifyIdentityRequest): ApiResponse<VerifyIdentityResponse> {
        val response = authApi.verifyIdentity(request)
        return response.body() ?: throw Exception(extractErrorMessage(response, "본인인증 요청에 실패했습니다."))
    }

    override suspend fun setupPin(
        verifyToken: String,
        pin: String,
        agreedConsentTypes: Set<String>
    ): ApiResponse<TokenResponse> {
        val response = authApi.setupPin(
            verifyToken,
            SetupPinRequest(
                pin = pin,
                deviceId = currentDeviceId(),
                deviceName = currentDeviceName(),
                consents = agreedConsentTypes.map { ConsentRequest(it, true) }
            )
        )
        val body = response.body() ?: throw Exception(extractErrorMessage(response, "PIN 설정에 실패했습니다."))
        saveIssuedTokens(body.data)
        return body
    }

    override suspend fun login(phoneNumber: String, pin: String): ApiResponse<TokenResponse> {
        val response = authApi.login(
            LoginRequest(
                phoneNumber = phoneNumber,
                pin = pin,
                deviceId = currentDeviceId(),
                deviceName = currentDeviceName()
            )
        )
        val body = response.body() ?: throw Exception(extractErrorMessage(response, "로그인에 실패했습니다."))
        saveIssuedTokens(body.data)
        return body
    }

    override suspend fun reissue(refreshToken: String): ApiResponse<TokenResponse> {
        val response = authApi.reissue(ReissueRequest(refreshToken))
        val body = response.body() ?: throw Exception(extractErrorMessage(response, "토큰 재발급에 실패했습니다."))
        saveIssuedTokens(body.data)
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

    private fun saveIssuedTokens(tokenResponse: TokenResponse?) {
        if (tokenResponse?.requiresAdditionalAuth == true) {
            throw Exception("추가 본인인증이 필요합니다.")
        }
        val accessToken = tokenResponse?.accessToken
        val refreshToken = tokenResponse?.refreshToken
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            throw Exception("토큰 응답이 비어 있습니다.")
        }
        saveTokens(accessToken, refreshToken)
    }

    private fun currentDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
    }

    private fun currentDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        return listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android" }
    }

    private fun extractErrorMessage(response: Response<*>, fallback: String): String {
        val raw = response.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return fallback
        if (raw.trimStart().startsWith("<")) return fallback
        return runCatching {
            JSONObject(raw).optString("message").takeIf { it.isNotBlank() } ?: fallback
        }.getOrDefault(fallback)
    }
}
