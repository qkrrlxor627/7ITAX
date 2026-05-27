package com.ssafy.tax7i.bookentry.repository;

/**
 * aggregateByUserIdAndDateRange 쿼리 결과를 안전하게 추출하는 DTO.
 * <p>
 * 반환 순서: [income, expense, asset, businessExpense]
 * JPA 구현체에 따라 Object[] 또는 Object[][] 로 반환될 수 있어 unwrap 처리 포함.
 */
public record AggregateResult(
        long totalIncome,
        long totalExpense,
        long totalAsset,
        long businessExpense
) {

    public static AggregateResult from(Object[] raw) {
        Object[] agg = raw;
        if (raw != null && raw.length > 0 && raw[0] instanceof Object[]) {
            agg = (Object[]) raw[0];
        }
        return new AggregateResult(
                extractLong(agg, 0),
                extractLong(agg, 1),
                extractLong(agg, 2),
                extractLong(agg, 3)
        );
    }

    private static long extractLong(Object[] arr, int idx) {
        return arr != null && arr.length > idx && arr[idx] != null
                ? ((Number) arr[idx]).longValue()
                : 0L;
    }
}
