package com.ssafy.seveniTax.data.model.classification

data class ClassificationRequest(
    val merchantName: String,
    val mcc: String? = null,
    val amount: Long? = null,
    val isBusinessPurpose: Boolean? = null,
    val isClientAccompanied: Boolean? = null,
    val note: String? = null
)
