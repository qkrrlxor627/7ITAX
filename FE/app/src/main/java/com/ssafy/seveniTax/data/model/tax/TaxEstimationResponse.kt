package com.ssafy.seveniTax.data.model.tax

data class TaxEstimationResponse(
    val year: Int,
    val totalIncome: Long,
    val totalExpense: Long,
    val totalAssetPurchase: Long,
    val taxableIncome: Long,
    val salesVat: Long,
    val purchaseVat: Long,
    val estimatedVat: Long,
    val estimatedIncomeTax: Long,
    val incomeTaxBracket: String,
    val estimatedLocalTax: Long,
    val deductibleExpenses: Long,
    val taxSavingFromExpenses: Long
)
