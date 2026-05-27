package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyCreditCardProductListRequest(
        @JsonProperty("Header") SsafyCommonHeader header
) {
}
