package com.ssafy.tax7i.payment.dto;

import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentCaptureResponse(
        Long paymentId,
        PaymentStatus status,
        Long cardId,
        Long amount,
        LocalDateTime capturedAt
) {
    public static PaymentCaptureResponse of(Payment payment) {
        return new PaymentCaptureResponse(
                payment.getId(),
                payment.getStatus(),
                payment.getCard().getId(),
                payment.getAmount(),
                payment.getCapturedAt()
        );
    }
}
