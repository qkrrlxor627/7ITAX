package com.ssafy.tax7i.payment.dto;

import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentPurpose;
import com.ssafy.tax7i.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentAuthorizeResponse(
        Long paymentId,
        String authorizationCode,
        Long cardId,
        Long amount,
        PaymentStatus status,
        PaymentPurpose purpose,
        LocalDateTime authorizedAt
) {
    public static PaymentAuthorizeResponse from(Payment payment) {
        return new PaymentAuthorizeResponse(
                payment.getId(),
                payment.getAuthorizationCode(),
                payment.getCard().getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPurpose(),
                payment.getAuthorizedAt()
        );
    }
}
