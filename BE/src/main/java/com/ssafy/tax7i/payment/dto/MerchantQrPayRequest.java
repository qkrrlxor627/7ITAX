package com.ssafy.tax7i.payment.dto;

import jakarta.validation.constraints.NotNull;

public record MerchantQrPayRequest(
        @NotNull(message = "카드 ID는 필수입니다.")
        Long cardId
) {}
