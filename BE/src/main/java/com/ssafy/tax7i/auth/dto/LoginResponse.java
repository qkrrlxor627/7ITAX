package com.ssafy.tax7i.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
