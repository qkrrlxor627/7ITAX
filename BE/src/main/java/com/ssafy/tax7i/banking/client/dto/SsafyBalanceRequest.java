package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyBalanceRequest(
        @JsonProperty("Header") SsafyCommonHeader header,
        @JsonProperty("accountNo") String accountNo
) {
}
