package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyWithdrawRequest(
        @JsonProperty("Header") SsafyCommonHeader header,
        @JsonProperty("accountNo") String accountNo,
        @JsonProperty("transactionBalance") String transactionBalance,
        @JsonProperty("transactionSummary") String transactionSummary
) {
}
