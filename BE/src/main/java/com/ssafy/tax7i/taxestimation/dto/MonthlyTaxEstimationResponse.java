package com.ssafy.tax7i.taxestimation.dto;

/**
 * 월별 세금 추정 응답
 *
 * 세법 근거:
 * - 부가세: 부가가치세법 §49 (과세기간 종료 후 25일 이내 확정신고)
 * - 종소세: 소득세법 §70 (귀속연도 다음해 5.1~5.31)
 * - 지방세: 지방세법 §95 (종소세와 동시 5.1~5.31)
 * - 불공제: 부가가치세법 §39①1 (접대비 관련 매입세액)
 */
public record MonthlyTaxEstimationResponse(
        int year,
        int month,
        VatEstimation estimatedVat,
        IncomeTaxEstimation estimatedIncomeTax,
        LocalTaxEstimation estimatedLocalTax,
        long totalEstimatedTax
) {
    public record VatEstimation(
            String period,
            long salesTax,
            long purchaseTax,
            long estimatedPayable,
            String dueDate
    ) {}

    public record IncomeTaxEstimation(
            long currentIncome,
            long currentExpense,
            long projectedAnnualIncome,
            long projectedAnnualExpense,
            long estimatedTax,
            String dueDate
    ) {}

    public record LocalTaxEstimation(
            long amount,
            String dueDate
    ) {}
}
