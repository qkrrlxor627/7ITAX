package com.ssafy.tax7i.sms.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpVerifyRequest(
        @NotBlank(message = "인증번호는 필수입니다.")
        String otpCode
) {}
