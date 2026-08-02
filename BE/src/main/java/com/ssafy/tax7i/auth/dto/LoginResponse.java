package com.ssafy.tax7i.auth.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        boolean requiresAdditionalAuth,
        String additionalAuthToken,
        String maskedPhone,
        Integer otpExpiresInSeconds,
        List<String> riskReasons
) {
    public LoginResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, false, null, null, null, List.of());
    }

    public static LoginResponse additionalAuthRequired(
            String additionalAuthToken,
            String maskedPhone,
            Integer otpExpiresInSeconds,
            List<String> riskReasons
    ) {
        return new LoginResponse(null, null, true, additionalAuthToken, maskedPhone, otpExpiresInSeconds, riskReasons);
    }
}
