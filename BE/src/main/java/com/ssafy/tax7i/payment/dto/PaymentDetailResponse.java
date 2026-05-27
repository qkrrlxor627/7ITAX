package com.ssafy.tax7i.payment.dto;

import com.ssafy.tax7i.payment.entity.*;

import java.time.LocalDateTime;

public record PaymentDetailResponse(
        Long paymentId,
        Long cardId,
        Long amount,
        String currency,
        String merchantName,
        String merchantCategoryCode,
        PaymentMethod paymentMethod,
        PaymentPurpose purpose,
        PaymentStatus status,
        String authorizationCode,
        Long cancelledAmount,
        String cancelReason,
        LocalDateTime authorizedAt,
        LocalDateTime capturedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt
) {
    public static PaymentDetailResponse from(Payment payment) {
        return new PaymentDetailResponse(
                payment.getId(),
                payment.getCard().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMerchantName(),
                payment.getMerchantCategoryCode(),
                payment.getPaymentMethod(),
                payment.getPurpose(),
                payment.getStatus(),
                payment.getAuthorizationCode(),
                payment.getCancelledAmount(),
                payment.getCancelReason(),
                payment.getAuthorizedAt(),
                payment.getCapturedAt(),
                payment.getCancelledAt(),
                payment.getCreatedAt()
        );
    }
}
