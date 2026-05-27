package com.ssafy.tax7i.card.dto;

public record CardPaymentResponse(
        Long transactionUniqueNo,
        Long cardId,
        String merchantName,
        String categoryName,
        Long paymentBalance,
        String transactionDate,
        String transactionTime
) {}
