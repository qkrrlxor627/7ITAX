package com.ssafy.tax7i.auth.dto;

public record IdentityVerifyResponse(
        Long userId,
        boolean isNewUser,
        boolean requiresPinSetup,
        String verifyToken
) {}
