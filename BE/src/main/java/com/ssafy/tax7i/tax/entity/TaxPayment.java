package com.ssafy.tax7i.tax.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Entity
@Table(name = "tax_payments", indexes = {
        @Index(name = "idx_tax_payment_tax_return", columnList = "tax_return_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_tax_payment_return_type", columnNames = {"tax_return_id", "payment_type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxPayment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_return_id", nullable = false)
    private TaxReturn taxReturn;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private TaxPaymentType paymentType;

    @Column(nullable = false)
    private long amount;

    @Column(name = "virtual_account")
    private String virtualAccount;

    @Column(name = "virtual_bank")
    private String virtualBank;

    @Column(name = "from_account")
    private String fromAccount;

    @Column(name = "transfer_id")
    private String transferId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxPaymentStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    public TaxPayment(TaxReturn taxReturn, TaxPaymentType paymentType, long amount,
                      String virtualAccount, String virtualBank,
                      String fromAccount, String transferId,
                      TaxPaymentStatus status, String errorMessage, LocalDateTime paidAt) {
        this.taxReturn = taxReturn;
        this.paymentType = paymentType;
        this.amount = amount;
        this.virtualAccount = virtualAccount;
        this.virtualBank = virtualBank != null ? virtualBank : "싸피뱅크";
        this.fromAccount = fromAccount;
        this.transferId = transferId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.paidAt = paidAt;
    }

    public void markProcessing() {
        if (this.status != TaxPaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.TAX_ALREADY_PAID,
                    "결제 상태가 PENDING이 아닙니다: " + this.status);
        }
        this.status = TaxPaymentStatus.PROCESSING;
    }

    public void complete(String transferId, String fromAccount) {
        if (this.status != TaxPaymentStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.TAX_ALREADY_PAID,
                    "결제 상태가 PROCESSING이 아닙니다: " + this.status);
        }
        this.status = TaxPaymentStatus.COMPLETED;
        this.transferId = transferId;
        this.fromAccount = fromAccount;
        this.paidAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        if (this.status != TaxPaymentStatus.PROCESSING) {
            log.warn("fail() 호출 무시: 현재 상태={}, id={}", this.status, this.id);
            return;
        }
        this.status = TaxPaymentStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
