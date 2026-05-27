package com.ssafy.tax7i.payment.dto;

import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentCancelResponse(
        Long paymentId,
        PaymentStatus status,
        Long cancelledAmount,
        Long cardId,
        LocalDateTime cancelledAt
) {
    public static PaymentCancelResponse from(Payment payment) {
        return new PaymentCancelResponse(
                payment.getId(),
                payment.getStatus(),
                payment.getCancelledAmount(),
                payment.getCard().getId(),
                payment.getCancelledAt()
        );
    }
}
