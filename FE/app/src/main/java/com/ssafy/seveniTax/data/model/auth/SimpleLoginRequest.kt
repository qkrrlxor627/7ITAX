package com.ssafy.seveniTax.data.model.auth

data class SimpleLoginRequest(
    val phoneNumber: String,
    val name: String? = null
)
