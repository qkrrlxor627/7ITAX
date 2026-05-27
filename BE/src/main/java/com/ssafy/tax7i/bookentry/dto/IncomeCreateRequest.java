package com.ssafy.tax7i.bookentry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record IncomeCreateRequest(
        @NotNull(message = "거래일자는 필수입니다.")
        LocalDate transactionDate,

        @NotBlank(message = "설명은 필수입니다.")
        String description,

        @NotNull(message = "금액은 필수입니다.")
        @Positive(message = "금액은 양수여야 합니다.")
        Long amount,

        Double withholdingRate,

        String note
) {}
