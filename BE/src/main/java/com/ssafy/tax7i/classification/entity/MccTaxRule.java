package com.ssafy.tax7i.classification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "mcc_tax_rule", indexes = {
        @Index(name = "idx_mcc_tax_rule_mcc", columnList = "mcc"),
        @Index(name = "idx_mcc_tax_rule_tier", columnList = "tier")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MccTaxRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String mcc;

    @Column(nullable = false, length = 1)
    private String tier; // A: 자동확정, B: 조건분기

    @Column(columnDefinition = "TEXT")
    private String conditionExpr;

    @Column(nullable = false, length = 50)
    private String taxCategory;

    @Column(nullable = false, length = 30)
    private String vatDeductible;

    private Long amountThreshold;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(length = 200)
    private String legalBasis;

    private Long annualLimit;

    /** 분류 신뢰도 (0~100). seed.xlsx MCC_매핑룰_v3 기준 */
    private Integer confidence;

    /** 사용자 확인 필요 여부. true면 UI에서 확인 필수 */
    @Column(nullable = false)
    private Boolean requiresUserConfirm = false;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public boolean isTierA() {
        return "A".equals(tier);
    }

    public boolean isAmountCondition() {
        return conditionExpr != null && conditionExpr.startsWith("AMOUNT_");
    }

    public boolean isMerchantKeywordCondition() {
        return conditionExpr != null && conditionExpr.startsWith("MERCHANT_LIKE:");
    }

    public boolean isUserChoiceCondition() {
        return conditionExpr != null &&
                !isAmountCondition() && !isMerchantKeywordCondition();
    }
}
