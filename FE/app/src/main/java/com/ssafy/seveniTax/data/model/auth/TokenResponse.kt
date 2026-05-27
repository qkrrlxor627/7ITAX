package com.ssafy.seveniTax.data.model.auth

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)
