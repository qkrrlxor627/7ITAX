package com.ssafy.seveniTax.data.model.auth

data class SimpleLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val name: String,
    val phoneNumber: String,
    val newUser: Boolean
)
