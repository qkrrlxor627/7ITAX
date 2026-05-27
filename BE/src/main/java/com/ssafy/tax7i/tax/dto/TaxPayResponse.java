package com.ssafy.tax7i.tax.dto;

import com.ssafy.tax7i.tax.entity.TaxPayment;
import com.ssafy.tax7i.tax.entity.TaxPaymentStatus;
import com.ssafy.tax7i.tax.entity.TaxPaymentType;

import java.time.LocalDateTime;

public record TaxPayResponse(
        TaxPaymentType paymentType,
        long amount,
        String transferId,
        TaxPaymentStatus status,
        LocalDateTime paidAt
) {
    public static TaxPayResponse from(TaxPayment payment) {
        return new TaxPayResponse(
                payment.getPaymentType(),
                payment.getAmount(),
                payment.getTransferId(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
