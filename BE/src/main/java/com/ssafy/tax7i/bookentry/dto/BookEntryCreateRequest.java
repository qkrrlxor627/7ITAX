package com.ssafy.tax7i.bookentry.dto;

import com.ssafy.tax7i.bookentry.entity.EntryType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record BookEntryCreateRequest(
        Long paymentId,

        @NotNull(message = "거래일자는 필수입니다.")
        LocalDate entryDate,

        String description,

        String merchantName,

        @NotNull(message = "거래유형은 필수입니다.")
        EntryType entryType,

        @Positive(message = "금액은 양수여야 합니다.")
        Long amount,

        Boolean isVatExempt,

        String categoryCode,

        String categoryName,

        @Min(0) @Max(100) Integer confidenceScore,

        String note
) {
    public boolean isVatExemptOrDefault() {
        return isVatExempt != null && isVatExempt;
    }
}
