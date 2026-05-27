package com.ssafy.tax7i.card.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CardPaymentRequest(
        @NotNull(message = "가맹점 ID는 필수입니다.")
        Long merchantId,

        @NotNull(message = "결제 금액은 필수입니다.")
        @Positive(message = "결제 금액은 0보다 커야 합니다.")
        Long paymentBalance
) {}
