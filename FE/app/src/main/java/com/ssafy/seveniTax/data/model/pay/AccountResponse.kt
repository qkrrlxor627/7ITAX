package com.ssafy.seveniTax.data.model.pay

data class AccountResponse(
    val id: Long,
    val accountType: String,
    val bankCode: String,
    val accountNumber: String,
    val alias: String?,
    val balance: Long,
    val status: String,
    val createdAt: String
)
