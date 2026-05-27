package com.ssafy.tax7i.banking.dto;

import java.time.LocalDateTime;

public record BalanceResponse(
        Long accountId,
        Long balance,
        Long availableBalance,
        Long pendingAmount,
        LocalDateTime asOf
) {
    public static BalanceResponse of(Long accountId, long balance) {
        return new BalanceResponse(
                accountId,
                balance,
                balance,      // SSAFY는 가용잔액 별도 없음 → balance와 동일
                0L,           // SSAFY는 보류금액 없음 → 0
                LocalDateTime.now()
        );
    }
}
