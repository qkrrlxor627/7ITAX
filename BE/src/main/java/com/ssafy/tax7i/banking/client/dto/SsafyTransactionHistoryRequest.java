package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyTransactionHistoryRequest(
        @JsonProperty("Header") SsafyCommonHeader header,
        @JsonProperty("accountNo") String accountNo,
        @JsonProperty("startDate") String startDate,
        @JsonProperty("endDate") String endDate,
        @JsonProperty("transactionType") String transactionType,
        @JsonProperty("orderByType") String orderByType
) {}
