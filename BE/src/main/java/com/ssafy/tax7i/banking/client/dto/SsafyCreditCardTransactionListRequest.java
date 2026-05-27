package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyCreditCardTransactionListRequest(
        @JsonProperty("Header") SsafyCommonHeader header,
        @JsonProperty("cardNo") String cardNo,
        @JsonProperty("cvc") String cvc,
        @JsonProperty("startDate") String startDate,
        @JsonProperty("endDate") String endDate
) {
}
