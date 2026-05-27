package com.ssafy.seveniTax.data.model.card

data class CardResponse(
    val id: Long,
    val cardName: String,
    val cardType: String,
    val last4Digits: String,
    val isDefault: Boolean,
    val cardExpiryDate: String?,
    val withdrawalDate: String?,
    val createdAt: String?
)
