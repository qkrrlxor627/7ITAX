package com.ssafy.tax7i.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PinSetupRequest(
        @NotBlank(message = "PIN은 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "PIN은 6자리 숫자여야 합니다.")
        String pin
) {
}
