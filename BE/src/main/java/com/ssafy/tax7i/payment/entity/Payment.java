package com.ssafy.tax7i.payment.entity;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_card_id", columnList = "card_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_user_status", columnList = "user_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String currency;

    private Long merchantId;

    @Column(nullable = false)
    private String merchantName;

    private String merchantCategoryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String authorizationCode;

    private Long cancelledAmount;

    private String cancelReason;

    private Long ssafyTransactionUniqueNo;

    private LocalDateTime authorizedAt;

    private LocalDateTime capturedAt;

    private LocalDateTime cancelledAt;

    @Builder
    public Payment(User user, Card card, Long amount, String currency,
                   Long merchantId, String merchantName, String merchantCategoryCode,
                   PaymentMethod paymentMethod, PaymentPurpose purpose,
                   String authorizationCode) {
        this.user = user;
        this.card = card;
        this.amount = amount;
        this.currency = currency;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCategoryCode = merchantCategoryCode;
        this.paymentMethod = paymentMethod;
        this.purpose = purpose;
        this.authorizationCode = authorizationCode;
        this.status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = LocalDateTime.now();
    }

    public void capture() {
        this.status = PaymentStatus.CAPTURED;
        this.capturedAt = LocalDateTime.now();
    }

    public void cancel(Long cancelAmount, String reason) {
        this.cancelledAmount = (this.cancelledAmount != null ? this.cancelledAmount : 0L) + cancelAmount;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
        if (this.cancelledAmount >= this.amount) {
            this.status = PaymentStatus.CANCELLED;
        }
    }

    public void assignSsafyTransaction(Long transactionUniqueNo) {
        this.ssafyTransactionUniqueNo = transactionUniqueNo;
    }

    public void decline() {
        if (this.status != PaymentStatus.AUTHORIZED) return;
        this.status = PaymentStatus.DECLINED;
    }
}
