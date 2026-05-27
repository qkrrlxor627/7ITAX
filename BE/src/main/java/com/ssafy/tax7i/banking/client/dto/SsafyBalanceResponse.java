package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyBalanceResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") BalanceRec rec
) implements SsafyApiResponse {
    public record BalanceRec(
            @JsonProperty("bankCode") String bankCode,
            @JsonProperty("accountNo") String accountNo,
            @JsonProperty("accountBalance") String accountBalance,
            @JsonProperty("lastTransactionDate") String lastTransactionDate
    ) {
    }
}
