package com.ssafy.tax7i.payment.dto;

import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentPurpose;
import com.ssafy.tax7i.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record QrPaymentResponse(
        Long paymentId,
        String authorizationCode,
        String merchantName,
        Long amount,
        PaymentStatus status,
        PaymentPurpose purpose,
        LocalDateTime capturedAt
) {
    public static QrPaymentResponse of(Payment payment) {
        return new QrPaymentResponse(
                payment.getId(),
                payment.getAuthorizationCode(),
                payment.getMerchantName(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPurpose(),
                payment.getCapturedAt()
        );
    }
}
