package com.ssafy.seveniTax.data.model.pay

data class AccountCreateRequest(
    val accountType: String,
    val bankCode: String,
    val alias: String? = null
)
