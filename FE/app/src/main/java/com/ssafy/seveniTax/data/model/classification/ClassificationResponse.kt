package com.ssafy.seveniTax.data.model.classification

data class ClassificationResponse(
    val confidence: String,         // CONFIRMED / RECOMMENDED / NEEDS_CONFIRMATION
    val taxCategory: String,
    val vatDeductible: String,
    val legalBasis: String,
    val remark: String,
    val entertainmentLimit: EntertainmentLimit? = null
)

data class EntertainmentLimit(
    val annualLimit: Long,
    val usedAmount: Long,
    val remainingAmount: Long,
    val isOverLimit: Boolean
)
