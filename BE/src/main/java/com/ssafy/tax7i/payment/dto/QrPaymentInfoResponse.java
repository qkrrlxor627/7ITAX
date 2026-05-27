package com.ssafy.tax7i.payment.dto;

import com.ssafy.tax7i.payment.entity.PaymentPurpose;
import com.ssafy.tax7i.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record QrPaymentInfoResponse(
        String token,
        String payerName,
        Long amount,
        String merchantName,
        PaymentPurpose purpose,
        PaymentStatus status,
        LocalDateTime createdAt
) {}
