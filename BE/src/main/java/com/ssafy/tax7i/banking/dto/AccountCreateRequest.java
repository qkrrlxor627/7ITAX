package com.ssafy.tax7i.banking.dto;

import com.ssafy.tax7i.banking.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountCreateRequest(
        @NotNull(message = "계좌 유형은 필수입니다.")
        AccountType accountType,

        @NotBlank(message = "은행 코드는 필수입니다.")
        String bankCode,

        String alias           // nullable
) {}
