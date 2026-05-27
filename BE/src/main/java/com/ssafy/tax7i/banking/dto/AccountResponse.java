package com.ssafy.tax7i.banking.dto;

import com.ssafy.tax7i.banking.entity.Account;

import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String accountType,       // enum.name() → "PERSONAL" | "BUSINESS"
        String bankCode,
        String accountNumber,
        String alias,
        Long balance,
        String status,            // enum.name() → "ACTIVE" | "INACTIVE" | "CLOSED"
        LocalDateTime createdAt
) {
    public static AccountResponse from(Account account, Long balance) {
        return new AccountResponse(
                account.getId(),
                account.getAccountType().name(),
                account.getBankCode(),
                account.getAccountNo(),
                account.getAlias(),
                balance,
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }
}
