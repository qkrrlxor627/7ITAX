package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyDepositResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") DepositRec rec
) implements SsafyApiResponse {
    public record DepositRec(
            @JsonProperty("transactionUniqueNo") String transactionUniqueNo,
            @JsonProperty("accountNo") String accountNo,
            @JsonProperty("transactionDate") String transactionDate,
            @JsonProperty("transactionBalance") String transactionBalance,
            @JsonProperty("accountBalance") String accountBalance
    ) {
    }
}
