package com.ssafy.tax7i.card.dto;

import com.ssafy.tax7i.card.entity.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCardFromAccountRequest(
        @NotBlank(message = "카드 이름은 필수입니다.")
        String cardName,

        @NotNull(message = "카드 유형은 필수입니다.")
        CardType cardType,

        @NotBlank(message = "계좌번호는 필수입니다.")
        String accountNo
) {}
