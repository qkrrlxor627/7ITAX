package com.ssafy.tax7i.card.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 로컬 카드 시뮬레이션의 카드 결제 원장(ledger).
 * 외부 SSAFY 신용카드 API 대신 자체 DB에 카드 결제/취소 내역을 기록한다.
 *
 * <p>참고: 기존 {@code CardTransaction} 엔티티는 결제 서비스(PaymentService)에서
 * 카드 거래 요약을 별도로 기록하는 용도이며, 본 엔티티와는 목적/스키마가 다르다.
 */
@Entity
@Table(name = "card_ledger_transactions", indexes = {
        @Index(name = "idx_card_ledger_card_no_key", columnList = "card_no_key"),
        @Index(name = "idx_card_ledger_tx_unique_no", columnList = "transaction_unique_no")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardLedgerTransaction extends BaseTimeEntity {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_no_key", nullable = false)
    private String cardNoKey;

    @Column(name = "transaction_unique_no", nullable = false)
    private String transactionUniqueNo;

    private Long merchantId;

    private String merchantName;

    private String categoryId;

    private String categoryName;

    @Column(nullable = false)
    private long paymentBalance;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime transactedAt;

    @Builder
    public CardLedgerTransaction(String cardNoKey, String transactionUniqueNo, Long merchantId, String merchantName,
                                 String categoryId, String categoryName, long paymentBalance, String status,
                                 LocalDateTime transactedAt) {
        this.cardNoKey = cardNoKey;
        this.transactionUniqueNo = transactionUniqueNo;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.paymentBalance = paymentBalance;
        this.status = status;
        this.transactedAt = transactedAt;
    }

    public void cancel() {
        this.status = STATUS_CANCELLED;
    }
}
