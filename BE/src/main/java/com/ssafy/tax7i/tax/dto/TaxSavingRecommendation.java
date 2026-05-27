package com.ssafy.tax7i.tax.dto;

public record TaxSavingRecommendation(
        String type,
        String name,
        String description,
        long maxAmount,
        long estimatedSaving,
        boolean isApplied,
        long usedAmount,
        long remainingAmount,
        double usageRate
) {
    /** 기존 코드 호환용 — usedAmount/remainingAmount/usageRate 없이 생성 */
    public TaxSavingRecommendation(String type, String name, String description,
                                    long maxAmount, long estimatedSaving, boolean isApplied) {
        this(type, name, description, maxAmount, estimatedSaving, isApplied, 0, maxAmount, 0.0);
    }

    /** usedAmount 기반 자동 계산 생성자 */
    public static TaxSavingRecommendation withUsage(String type, String name, String description,
                                                     long maxAmount, long estimatedSaving,
                                                     boolean isApplied, long usedAmount) {
        long remaining = Math.max(0, maxAmount - usedAmount);
        double rate = maxAmount == 0 ? 0.0 : Math.round(usedAmount * 1000.0 / maxAmount) / 10.0;
        return new TaxSavingRecommendation(type, name, description, maxAmount,
                estimatedSaving, isApplied, usedAmount, remaining, rate);
    }
}
