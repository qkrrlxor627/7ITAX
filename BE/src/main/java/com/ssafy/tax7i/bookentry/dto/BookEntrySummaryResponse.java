package com.ssafy.tax7i.bookentry.dto;

import java.util.List;

public record BookEntrySummaryResponse(
        int year,
        long totalIncome,
        long totalExpense,
        List<MonthSummary> byMonth,
        List<CategorySummary> byCategory,
        List<MerchantSummary> byMerchant,
        YearOverYearComparison comparison
) {
    public record MonthSummary(int month, long income, long expense, long asset) {}

    public record CategorySummary(String code, String name, long amount, long count, double percentage) {}

    public record MerchantSummary(String merchantName, long totalAmount, long count, double percentage) {}

    /**
     * 전년 대비 비교. 전년 데이터가 없으면 null.
     * changeRate = ((올해 - 전년) / 전년) × 100, 소수점 1자리
     */
    public record YearOverYearComparison(
            long prevIncome,
            long prevExpense,
            long prevProfit,
            Double incomeChangeRate,
            Double expenseChangeRate,
            Double profitChangeRate
    ) {}
}
