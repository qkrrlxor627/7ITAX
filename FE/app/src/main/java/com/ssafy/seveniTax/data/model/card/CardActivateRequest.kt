package com.ssafy.seveniTax.data.model.card

data class CardActivateRequest(
    val activationCode: String
)

data class CardActivateResponse(
    val id: Long,
    val status: String,
    val activatedAt: String?
)
