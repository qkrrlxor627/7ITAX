package com.ssafy.seveniTax.data.model.card

data class CardProductResponse(
    val cardUniqueNo: String,
    val cardIssuerName: String,
    val cardName: String,
    val baselinePerformance: String,
    val maxBenefitLimit: String,
    val cardDescription: String
)
