package com.ssafy.tax7i.card.dto;

import jakarta.validation.constraints.NotNull;

public record CancelPaymentRequest(
        @NotNull(message = "거래 고유번호는 필수입니다.")
        Long transactionUniqueNo
) {}
