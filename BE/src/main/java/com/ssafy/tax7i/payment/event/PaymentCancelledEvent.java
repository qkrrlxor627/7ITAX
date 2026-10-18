package com.ssafy.tax7i.payment.event;

public record PaymentCancelledEvent(
        Long paymentId,
        Long userId,
        Long amount,
        Long remainingAmount, // 이번 취소까지 반영한 남은 결제금액(0이면 전액 취소)
        String merchantName
) {}
