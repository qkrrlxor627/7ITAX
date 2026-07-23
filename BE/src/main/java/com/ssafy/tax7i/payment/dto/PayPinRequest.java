package com.ssafy.tax7i.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PayPinRequest(
        @NotBlank(message = "결제 비밀번호는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "결제 비밀번호는 6자리 숫자여야 합니다.")
        String pin
) {
}
