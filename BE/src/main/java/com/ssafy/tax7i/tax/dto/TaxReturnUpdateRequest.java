package com.ssafy.tax7i.tax.dto;

import java.util.Map;

public record TaxReturnUpdateRequest(
        Long prepaidTax,
        Map<String, Long> deductions
) {
}
