package com.ssafy.seveniTax.data.model.book

data class BookEntryResponse(
    val id: Long,
    val paymentId: Long? = null,
    val entryDate: String,
    val description: String? = null,
    val merchantName: String? = null,
    val entryType: String,
    val incomeAmount: Long,
    val expenseAmount: Long,
    val fixedAssetAmount: Long,
    val vatAmount: Long,
    val supplyPrice: Long,
    val categoryCode: String? = null,
    val categoryName: String? = null,
    val isBusinessExpense: Boolean,
    val isVatDeductible: Boolean,
    val confirmed: Boolean,
    val confirmedAt: String? = null,
    val note: String? = null,
    val createdAt: String
)
