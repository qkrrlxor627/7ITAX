package com.ssafy.seveniTax.data.model.auth

data class TokenResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val requiresAdditionalAuth: Boolean = false,
    val additionalAuthToken: String? = null,
    val maskedPhone: String? = null,
    val otpExpiresInSeconds: Int? = null,
    val riskReasons: List<String> = emptyList()
)
