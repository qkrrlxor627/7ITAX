package com.ssafy.tax7i.sms.dto;

public record OtpSendResponse(
        String maskedPhone,
        int expiresInSeconds
) {}
