package com.ssafy.tax7i.auth.dto;

public record SimpleLoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String name,
        String phoneNumber,
        boolean newUser
) {}
