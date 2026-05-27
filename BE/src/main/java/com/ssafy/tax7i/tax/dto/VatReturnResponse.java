package com.ssafy.tax7i.tax.dto;

import com.ssafy.tax7i.tax.entity.TaxReturnStatus;
import com.ssafy.tax7i.tax.entity.VatReturn;

import java.time.LocalDateTime;

public record VatReturnResponse(
        Long id,
        int taxYear,
        int taxPeriod,
        TaxReturnStatus status,
        String receiptNumber,
        long salesAmount,
        long salesTax,
        long purchaseAmount,
        long purchaseTax,
        long preliminaryPaid,
        long finalTax,
        LocalDateTime submittedAt,
        LocalDateTime createdAt
) {
    public static VatReturnResponse from(VatReturn v) {
        return new VatReturnResponse(
                v.getId(),
                v.getTaxYear(),
                v.getTaxPeriod(),
                v.getStatus(),
                v.getReceiptNumber(),
                v.getSalesAmount(),
                v.getSalesTax(),
                v.getPurchaseAmount(),
                v.getPurchaseTax(),
                v.getPreliminaryPaid(),
                v.getFinalTax(),
                v.getSubmittedAt(),
                v.getCreatedAt()
        );
    }
}
