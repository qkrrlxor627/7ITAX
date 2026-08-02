package com.ssafy.seveniTax.data.model.auth

data class LoginRequest(
    val phoneNumber: String,
    val pin: String,
    val deviceId: String,
    val deviceName: String
)
