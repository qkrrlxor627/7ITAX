package com.ssafy.seveniTax.data.model.book

data class BookEntryRequest(
    val paymentId: Long? = null,
    val entryDate: String,
    val description: String? = null,
    val merchantName: String? = null,
    val entryType: String,          // INCOME / EXPENSE / ASSET
    val amount: Long? = null,
    val isVatExempt: Boolean? = null,
    val categoryCode: String? = null,
    val categoryName: String? = null,
    val note: String? = null
)

data class CategoryUpdateRequest(
    val categoryCode: String,
    val categoryName: String
)

data class NoteUpdateRequest(
    val note: String
)
