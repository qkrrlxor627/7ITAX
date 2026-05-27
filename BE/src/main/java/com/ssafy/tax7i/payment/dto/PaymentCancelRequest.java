package com.ssafy.tax7i.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PaymentCancelRequest(
        @Positive(message = "취소 금액은 0보다 커야 합니다.")
        Long cancelAmount,

        @NotBlank(message = "취소 사유는 필수입니다.")
        @Size(max = 500, message = "취소 사유는 500자 이내여야 합니다.")
        String reason
) {
}
