package com.ssafy.tax7i.tax.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vat_returns", indexes = {
        @Index(name = "idx_vat_return_user_year", columnList = "user_id, tax_year")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_vat_return_user_year_period",
                columnNames = {"user_id", "tax_year", "tax_period"}),
        @UniqueConstraint(name = "uk_vat_return_receipt", columnNames = "receipt_number")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VatReturn extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tax_year", nullable = false)
    private int taxYear;

    @Column(name = "tax_period", nullable = false)
    private int taxPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxReturnStatus status;

    @Column(name = "receipt_number", unique = true)
    private String receiptNumber;

    @Column(name = "sales_amount", nullable = false)
    private long salesAmount;

    @Column(name = "sales_tax", nullable = false)
    private long salesTax;

    @Column(name = "purchase_amount", nullable = false)
    private long purchaseAmount;

    @Column(name = "purchase_tax", nullable = false)
    private long purchaseTax;

    @Column(name = "preliminary_paid", nullable = false)
    private long preliminaryPaid;

    @Column(name = "final_tax", nullable = false)
    private long finalTax;

    private LocalDateTime submittedAt;

    @Version
    private Long version;

    @Builder
    public VatReturn(Long userId, int taxYear, int taxPeriod, TaxReturnStatus status,
                     String receiptNumber,
                     long salesAmount, long salesTax,
                     long purchaseAmount, long purchaseTax,
                     long preliminaryPaid, long finalTax) {
        this.userId = userId;
        this.taxYear = taxYear;
        this.taxPeriod = taxPeriod;
        this.status = status;
        this.receiptNumber = receiptNumber;
        this.salesAmount = salesAmount;
        this.salesTax = salesTax;
        this.purchaseAmount = purchaseAmount;
        this.purchaseTax = purchaseTax;
        this.preliminaryPaid = preliminaryPaid;
        this.finalTax = finalTax;
    }

    public void transitionTo(TaxReturnStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new BusinessException(ErrorCode.TAX_INVALID_TRANSITION,
                    this.status + " → " + next + " 전이가 불가합니다.");
        }
        this.status = next;
        if (next == TaxReturnStatus.SUBMITTED) {
            this.submittedAt = LocalDateTime.now();
        }
    }

    public void assignReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }
}
