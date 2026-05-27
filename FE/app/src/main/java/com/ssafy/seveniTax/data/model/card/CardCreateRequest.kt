package com.ssafy.seveniTax.data.model.card

data class CardCreateRequest(
    val cardName: String,
    val cardType: String,
    val cardUniqueNo: String,
    val withdrawalAccountNo: String,
    val withdrawalDate: String,
    val otpToken: String
)
