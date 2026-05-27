package com.ssafy.tax7i.payment.dto;

import com.ssafy.tax7i.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record QrPaymentStatusResponse(
        String token,
        PaymentStatus status,
        Long paymentId,
        LocalDateTime capturedAt
) {}
