package com.ssafy.seveniTax.data.model.payment

data class QrTokenCreateRequest(
    val cardId: Long,
    val amount: Long,
    val merchantId: Long,
    val merchantName: String,
    val merchantCategoryCode: String? = null,
    val purpose: String = "BUSINESS"
)

data class QrTokenCreateResponse(
    val token: String,
    val paymentId: Long,
    val amount: Long,
    val payerName: String,
    val expiresAt: String?
)

data class QrPaymentInfoResponse(
    val token: String,
    val payerName: String,
    val amount: Long,
    val merchantName: String,
    val purpose: String,
    val status: String,
    val createdAt: String?
)

data class QrPaymentResponse(
    val paymentId: Long,
    val authorizationCode: String,
    val merchantName: String,
    val amount: Long,
    val status: String,
    val purpose: String,
    val capturedAt: String?
)

data class QrPaymentStatusResponse(
    val token: String,
    val status: String,
    val paymentId: Long?,
    val capturedAt: String?
)

data class MerchantResponse(
    val merchantId: Long,
    val merchantName: String,
    val categoryId: String?,
    val categoryName: String?
)
