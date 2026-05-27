package com.ssafy.seveniTax.data.model.auth

data class VerifyIdentityResponse(
    val userId: Long,
    val isNewUser: Boolean,
    val requiresPinSetup: Boolean,
    val verifyToken: String
)
