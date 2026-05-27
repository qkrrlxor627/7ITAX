package com.ssafy.tax7i.payment.dto;

import java.time.LocalDateTime;

public record MerchantQrTokenResponse(
        String token,
        Long amount,
        String merchantName,
        LocalDateTime expiresAt
) {}
