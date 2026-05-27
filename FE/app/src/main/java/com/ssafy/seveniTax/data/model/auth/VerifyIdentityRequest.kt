package com.ssafy.seveniTax.data.model.auth

data class VerifyIdentityRequest(
    val name: String,
    val birthDate: String,
    val gender: String,
    val phoneNumber: String
)
