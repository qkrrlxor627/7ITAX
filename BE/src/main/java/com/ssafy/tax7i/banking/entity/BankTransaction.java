package com.ssafy.tax7i.banking.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 로컬 뱅킹 시뮬레이션의 계좌 거래 원장(ledger).
 * 외부 금융망 대신 자체 DB에 입출금 거래를 기록한다.
 */
@Entity
@Table(name = "bank_transactions", indexes = {
        @Index(name = "idx_bank_tx_account_no_key", columnList = "account_no_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankTransaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no_key", nullable = false)
    private String accountNoKey;

    @Column(nullable = false)
    private String transactionUniqueNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankTransactionType transactionType;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private long balanceAfter;

    private String summary;

    private String memo;

    @Column(nullable = false)
    private LocalDateTime transactedAt;

    @Builder
    public BankTransaction(String accountNoKey, String transactionUniqueNo, BankTransactionType transactionType,
                           long amount, long balanceAfter, String summary, String memo, LocalDateTime transactedAt) {
        this.accountNoKey = accountNoKey;
        this.transactionUniqueNo = transactionUniqueNo;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.summary = summary;
        this.memo = memo;
        this.transactedAt = transactedAt;
    }
}
