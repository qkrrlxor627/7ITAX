package com.ssafy.tax7i.card.dto;

import com.ssafy.tax7i.banking.client.dto.SsafyBillingStatementsResponse;

public record CardBillingResponse(
        String billingMonth,
        Long billingAmount,
        Long paidAmount,
        Long unpaidAmount,
        String billingDate,
        String paymentDueDate,
        String status
) {
    public static CardBillingResponse from(SsafyBillingStatementsResponse.BillingRec rec) {
        return new CardBillingResponse(
                rec.billingMonth(),
                rec.billingAmount(),
                rec.paidAmount(),
                rec.unpaidAmount(),
                rec.billingDate(),
                rec.paymentDueDate(),
                rec.status()
        );
    }
}
