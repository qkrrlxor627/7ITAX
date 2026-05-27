package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyDepositRequest(
        @JsonProperty("Header") SsafyCommonHeader header,
        @JsonProperty("accountNo") String accountNo,
        @JsonProperty("transactionBalance") String transactionBalance,
        @JsonProperty("transactionSummary") String transactionSummary
) {
}
