package com.ssafy.tax7i.taxestimation.dto;

public record TaxEstimationResponse(
        // 기간
        int year,

        // 매출·비용 집계
        long totalIncome,
        long totalExpense,
        long totalAssetPurchase,
        long taxableIncome,

        // 부가세 예상
        long salesVat,
        long purchaseVat,
        long estimatedVat,

        // 종합소득세 예상
        long estimatedIncomeTax,
        String incomeTaxBracket,
        long estimatedLocalTax,

        // 절세 효과
        long deductibleExpenses,
        long taxSavingFromExpenses,

        // 부가세 반기별 상세 (부가가치세법 §5, §49)
        VatByPeriod vatByPeriod,

        // 연간 총 예상 세금 합산
        long totalAnnualTax,

        // 세율 구간 상세 (소득세법 §55)
        BracketDetail bracketDetail
) {
    /** 부가가치세법 §5: 과세기간 1기(1~6월), 2기(7~12월) */
    public record VatByPeriod(
            VatPeriodDetail period1,
            VatPeriodDetail period2,
            long totalVat
    ) {}

    public record VatPeriodDetail(
            String label,
            long salesTax,
            long purchaseTax,
            long estimatedPayable,
            String dueDate
    ) {}

    /** 소득세법 §55 8단계 누진세율 */
    public record BracketDetail(
            String currentBracket,
            long currentBracketMin,
            long currentBracketMax,
            long taxableIncome,
            Long amountToNextBracket,
            String nextBracketRate
    ) {}
}
