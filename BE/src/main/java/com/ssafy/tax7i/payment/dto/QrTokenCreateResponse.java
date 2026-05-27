package com.ssafy.tax7i.payment.dto;

import java.time.LocalDateTime;

public record QrTokenCreateResponse(
        String token,
        Long paymentId,
        Long amount,
        String payerName,
        LocalDateTime expiresAt
) {}
