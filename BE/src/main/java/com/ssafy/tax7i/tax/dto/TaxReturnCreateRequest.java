package com.ssafy.tax7i.tax.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TaxReturnCreateRequest(
        @NotNull @Min(2000) @Max(2100) int taxYear,
        @Min(0) Long prepaidTax,
        Map<String, Long> deductions
) {
}
