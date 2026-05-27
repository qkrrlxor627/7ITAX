package com.ssafy.tax7i.payment.dto;

import com.ssafy.tax7i.payment.entity.PaymentPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QrTokenCreateRequest(
        @NotNull(message = "카드 ID는 필수입니다.")
        Long cardId,

        @NotNull(message = "결제 금액은 필수입니다.")
        @Positive(message = "결제 금액은 0보다 커야 합니다.")
        Long amount,

        @NotNull(message = "가맹점 ID는 필수입니다.")
        Long merchantId,

        @NotBlank(message = "가맹점명은 필수입니다.")
        String merchantName,

        String merchantCategoryCode,

        @NotNull(message = "결제 목적은 필수입니다.")
        PaymentPurpose purpose
) {}
