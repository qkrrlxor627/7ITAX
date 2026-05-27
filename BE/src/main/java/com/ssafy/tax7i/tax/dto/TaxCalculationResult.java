package com.ssafy.tax7i.tax.dto;

public record TaxCalculationResult(
        long totalRevenue,
        long totalExpense,
        long incomeAmount,
        long totalDeductions,
        long taxableIncome,
        double taxRate,
        long calculatedTax,
        long totalTaxCredits,
        long determinedTax,
        long prepaidTax,
        long finalTax,
        long localTax,
        boolean isRefund
) {
}
