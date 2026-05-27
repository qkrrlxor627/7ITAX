package com.ssafy.tax7i.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CardDepositRequest(
        @NotNull(message = "충전 금액은 필수입니다.")
        @Positive(message = "충전 금액은 0보다 커야 합니다.")
        Long amount,

        @NotBlank(message = "출금 계좌번호는 필수입니다.")
        String sourceAccountNo
) {}
