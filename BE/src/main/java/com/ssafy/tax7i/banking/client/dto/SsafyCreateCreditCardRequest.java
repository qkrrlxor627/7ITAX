package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyCreateCreditCardRequest(
        @JsonProperty("Header") SsafyCommonHeader header,
        @JsonProperty("cardUniqueNo") String cardUniqueNo,
        @JsonProperty("withdrawalAccountNo") String withdrawalAccountNo,
        @JsonProperty("withdrawalDate") String withdrawalDate
) {
}
