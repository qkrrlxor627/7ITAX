package com.ssafy.tax7i.tax.dto;

import com.ssafy.tax7i.tax.entity.TaxReturnStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaxReturnSubmitResponse(
        String receiptNumber,
        LocalDateTime submittedAt,
        TaxReturnStatus status,
        long finalTax,
        long localTax,
        long totalPayable,
        AccountInfo nationalAccount,
        AccountInfo localAccount,
        LocalDate paymentDeadline
) {
    public record AccountInfo(String bank, String account) {}
}
