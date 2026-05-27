package com.ssafy.tax7i.sms.dto;

public record OtpVerifyResponse(
        String otpToken,
        int expiresInSeconds
) {}
