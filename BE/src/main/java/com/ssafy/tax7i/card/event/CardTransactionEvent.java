package com.ssafy.tax7i.card.event;

import com.ssafy.tax7i.card.entity.CardTransactionType;

/**
 * 카드 거래 기록 요청. 결제 트랜잭션이 커밋된 뒤 CardTransactionSaveService가 비동기로 저장한다
 */
public record CardTransactionEvent(
        Long cardId,
        CardTransactionType type,
        Long amount,
        String description,
        Long paymentBalance
) {}
