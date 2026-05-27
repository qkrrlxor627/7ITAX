package com.ssafy.tax7i.tax.dto;

import jakarta.validation.constraints.NotNull;

public record TaxPayRequest(
        @NotNull Long cardId
) {
}
