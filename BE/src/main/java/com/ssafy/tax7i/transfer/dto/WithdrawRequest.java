package com.ssafy.tax7i.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WithdrawRequest(
        @NotNull(message = "카드 ID는 필수입니다.")
        Long cardId,

        @NotBlank(message = "출금 대상 계좌번호는 필수입니다.")
        String targetAccountNo,

        @NotNull(message = "출금 금액은 필수입니다.")
        @Positive(message = "출금 금액은 0보다 커야 합니다.")
        Long amount,

        String description
) {}
