package com.ssafy.tax7i.card.dto;

import com.ssafy.tax7i.card.entity.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCardRequest(
        @NotBlank(message = "카드 이름은 필수입니다.")
        String cardName,

        @NotNull(message = "카드 유형은 필수입니다.")
        CardType cardType,

        @NotBlank(message = "카드 상품 고유번호는 필수입니다.")
        String cardUniqueNo,

        @NotBlank(message = "출금 계좌번호는 필수입니다.")
        String withdrawalAccountNo,

        @NotBlank(message = "결제일은 필수입니다.")
        String withdrawalDate,

        @NotBlank(message = "OTP 인증 토큰은 필수입니다.")
        String otpToken
) {}
