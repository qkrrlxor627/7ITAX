package com.ssafy.tax7i.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record P2pTransferRequest(
        @NotNull(message = "보내는 카드 ID는 필수입니다.")
        Long senderCardId,

        @NotNull(message = "받는 사용자 ID는 필수입니다.")
        Long receiverUserId,

        @NotNull(message = "송금 금액은 필수입니다.")
        @Positive(message = "송금 금액은 0보다 커야 합니다.")
        Long amount,

        String description
) {}
