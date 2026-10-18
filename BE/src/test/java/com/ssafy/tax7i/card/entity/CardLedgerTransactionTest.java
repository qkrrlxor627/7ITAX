package com.ssafy.tax7i.card.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CardLedgerTransactionTest {

    private CardLedgerTransaction txn(long amount) {
        return CardLedgerTransaction.builder()
                .cardNoKey("card")
                .transactionUniqueNo("1")
                .paymentBalance(amount)
                .status(CardLedgerTransaction.STATUS_SUCCESS)
                .transactedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void 부분취소는_취소액만_환불하고_SUCCESS_유지() {
        CardLedgerTransaction t = txn(30000L);

        assertThat(t.cancel(10000L)).isEqualTo(10000L);
        assertThat(t.getRemainingBalance()).isEqualTo(20000L);
        assertThat(t.getStatus()).isEqualTo(CardLedgerTransaction.STATUS_SUCCESS);
    }

    @Test
    void 누적취소가_결제액에_도달하면_CANCELLED() {
        CardLedgerTransaction t = txn(30000L);
        t.cancel(10000L);

        assertThat(t.cancel(20000L)).isEqualTo(20000L);
        assertThat(t.getStatus()).isEqualTo(CardLedgerTransaction.STATUS_CANCELLED);
        assertThat(t.getRemainingBalance()).isZero();
    }

    @Test
    void 남은금액을_넘는_취소는_남은금액까지만_환불() {
        CardLedgerTransaction t = txn(30000L);
        t.cancel(25000L);

        assertThat(t.cancel(Long.MAX_VALUE)).isEqualTo(5000L);
        assertThat(t.cancel(1000L)).isZero();
    }
}
