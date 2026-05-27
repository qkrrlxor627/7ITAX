package com.ssafy.tax7i.card.dto;

import com.ssafy.tax7i.banking.client.dto.SsafyCreditCardTransactionListResponse;

public record CardTransactionResponse(
        Long transactionUniqueNo,
        String transactionDate,
        String transactionTime,
        String merchantName,
        String categoryName,
        Long paymentBalance,
        String status
) {
    public static CardTransactionResponse from(SsafyCreditCardTransactionListResponse.TransactionRec rec) {
        return new CardTransactionResponse(
                rec.transactionUniqueNo(),
                rec.transactionDate(),
                rec.transactionTime(),
                rec.merchantName(),
                rec.categoryName(),
                rec.paymentBalance(),
                rec.status()
        );
    }
}
