package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyBillingStatementsRequest(
        @JsonProperty("Header") SsafyCommonHeader header,
        @JsonProperty("cardNo") String cardNo,
        @JsonProperty("cvc") String cvc,
        @JsonProperty("startMonth") String startMonth,
        @JsonProperty("endMonth") String endMonth
) {
}
