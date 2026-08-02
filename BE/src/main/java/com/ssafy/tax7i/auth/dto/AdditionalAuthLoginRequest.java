package com.ssafy.tax7i.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdditionalAuthLoginRequest(
        @NotBlank(message = "추가 인증 토큰은 필수입니다.")
        String additionalAuthToken,

        @NotBlank(message = "인증번호는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "인증번호는 6자리 숫자여야 합니다.")
        String otpCode
) {
}
