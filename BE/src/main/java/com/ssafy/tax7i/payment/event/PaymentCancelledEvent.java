package com.ssafy.tax7i.payment.event;

public record PaymentCancelledEvent(
        Long paymentId,
        Long userId,
        Long amount,
        String merchantName
) {}
