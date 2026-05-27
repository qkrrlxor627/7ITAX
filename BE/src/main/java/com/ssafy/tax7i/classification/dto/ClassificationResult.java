package com.ssafy.tax7i.classification.dto;

public record ClassificationResult(
        Confidence confidence,
        int confidenceScore,       // 분류 신뢰도 (0~100). DB mcc_tax_rule.confidence 기준
        String taxCategory,
        String vatDeductible,
        String legalBasis,
        String remark,
        EntertainmentLimitInfo entertainmentLimit // 접대비인 경우에만
) {

    public enum Confidence {
        CONFIRMED,          // Tier A: MCC만으로 확정 (confidence ≥ 90)
        RECOMMENDED,        // Tier B: 조건 매칭으로 추천 (confidence 55~89)
        NEEDS_CONFIRMATION  // 사용자 확인 필요 또는 AI 분류 필요 (confidence < 55)
    }

    public record EntertainmentLimitInfo(
            long annualLimit,
            long usedAmount,
            long remainingAmount,
            boolean isOverLimit
    ) {
    }

    public static ClassificationResult confirmed(String taxCategory, String vatDeductible,
                                                  String legalBasis, String remark, int score) {
        return new ClassificationResult(
                Confidence.CONFIRMED, score, taxCategory, vatDeductible, legalBasis, remark, null);
    }

    public static ClassificationResult recommended(String taxCategory, String vatDeductible,
                                                    String legalBasis, String remark, int score) {
        return new ClassificationResult(
                Confidence.RECOMMENDED, score, taxCategory, vatDeductible, legalBasis, remark, null);
    }

    public static ClassificationResult needsConfirmation(String taxCategory, String vatDeductible,
                                                          String legalBasis, String remark) {
        return new ClassificationResult(
                Confidence.NEEDS_CONFIRMATION, 0, taxCategory, vatDeductible, legalBasis, remark, null);
    }

    public ClassificationResult withEntertainmentLimit(EntertainmentLimitInfo limit) {
        return new ClassificationResult(confidence, confidenceScore, taxCategory, vatDeductible,
                legalBasis, remark, limit);
    }
}
