package com.ssafy.seveniTax.data.remote

import com.ssafy.seveniTax.BuildConfig
import com.ssafy.seveniTax.data.local.SecureStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object ApiClient {

    private val publicAuthPaths = setOf(
        "/api/auth/verify-identity",
        "/api/auth/setup-pin",
        "/api/auth/login",
        "/api/auth/reissue",
        "/api/auth/logout"
    )

    fun buildOkHttpClient(secureStorage: SecureStorage): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("X-Verify-Token")
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .header("Content-Type", "application/json; charset=UTF-8")
                val isPublicAuthRequest = originalRequest.url.encodedPath in publicAuthPaths

                if (!isPublicAuthRequest) {
                    secureStorage.getAccessToken()?.let { token ->
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                }

                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // AI 챗봇 응답이 30~90초 걸린다(BE ai.read-timeout과 맞춤)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
