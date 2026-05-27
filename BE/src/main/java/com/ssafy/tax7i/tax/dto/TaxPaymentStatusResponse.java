package com.ssafy.tax7i.tax.dto;

import com.ssafy.tax7i.tax.entity.TaxPaymentStatus;

import java.time.LocalDateTime;

public record TaxPaymentStatusResponse(
        PaymentInfo national,
        PaymentInfo local,
        long totalPaid
) {
    public record PaymentInfo(TaxPaymentStatus status, long amount, LocalDateTime paidAt) {}
}
