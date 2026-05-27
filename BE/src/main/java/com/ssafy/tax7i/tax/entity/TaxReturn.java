package com.ssafy.tax7i.tax.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tax_returns", indexes = {
        @Index(name = "idx_tax_return_user_year", columnList = "user_id, tax_year")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_tax_return_receipt", columnNames = "receipt_number")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxReturn extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tax_year", nullable = false)
    private int taxYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxReturnStatus status;

    @Column(name = "receipt_number", unique = true)
    private String receiptNumber;

    private LocalDateTime submittedAt;

    private LocalDateTime acceptedAt;

    // Personal info (snapshot from User)
    private String taxpayerName;

    private String residentNumber;

    // Tax calculation results
    @Column(nullable = false)
    private long totalRevenue;

    @Column(nullable = false)
    private long totalExpense;

    @Column(nullable = false)
    private long incomeAmount;

    @Column(nullable = false)
    private long totalDeductions;

    @Column(nullable = false)
    private long taxableIncome;

    @Column(nullable = false)
    private double taxRate;

    @Column(nullable = false)
    private long calculatedTax;

    @Column(nullable = false)
    private long determinedTax;

    @Column(nullable = false)
    private long prepaidTax;

    @Column(nullable = false)
    private long finalTax;

    @Column(nullable = false)
    private long localTax;

    @Column(columnDefinition = "TEXT")
    private String deductionsJson;

    @Version
    private Long version;

    @Builder
    public TaxReturn(Long userId, int taxYear, TaxReturnStatus status,
                     String receiptNumber, String taxpayerName, String residentNumber,
                     long totalRevenue, long totalExpense, long incomeAmount,
                     long totalDeductions, long taxableIncome, double taxRate,
                     long calculatedTax, long determinedTax,
                     long prepaidTax, long finalTax, long localTax,
                     String deductionsJson) {
        this.userId = userId;
        this.taxYear = taxYear;
        this.status = status;
        this.receiptNumber = receiptNumber;
        this.taxpayerName = taxpayerName;
        this.residentNumber = residentNumber;
        this.totalRevenue = totalRevenue;
        this.totalExpense = totalExpense;
        this.incomeAmount = incomeAmount;
        this.totalDeductions = totalDeductions;
        this.taxableIncome = taxableIncome;
        this.taxRate = taxRate;
        this.calculatedTax = calculatedTax;
        this.determinedTax = determinedTax;
        this.prepaidTax = prepaidTax;
        this.finalTax = finalTax;
        this.localTax = localTax;
        this.deductionsJson = deductionsJson;
    }

    public void assignReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public void transitionTo(TaxReturnStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new BusinessException(ErrorCode.TAX_INVALID_TRANSITION,
                    this.status + " → " + next + " 전이가 불가합니다.");
        }
        this.status = next;
        if (next == TaxReturnStatus.SUBMITTED) {
            this.submittedAt = LocalDateTime.now();
        } else if (next == TaxReturnStatus.ACCEPTED) {
            this.acceptedAt = LocalDateTime.now();
        }
    }

    public void updateCalculation(long totalRevenue, long totalExpense, long incomeAmount,
                                  long totalDeductions, long taxableIncome, double taxRate,
                                  long calculatedTax, long determinedTax,
                                  long prepaidTax, long finalTax, long localTax,
                                  String deductionsJson) {
        this.totalRevenue = totalRevenue;
        this.totalExpense = totalExpense;
        this.incomeAmount = incomeAmount;
        this.totalDeductions = totalDeductions;
        this.taxableIncome = taxableIncome;
        this.taxRate = taxRate;
        this.calculatedTax = calculatedTax;
        this.determinedTax = determinedTax;
        this.prepaidTax = prepaidTax;
        this.finalTax = finalTax;
        this.localTax = localTax;
        this.deductionsJson = deductionsJson;
    }
}
