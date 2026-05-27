package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyAccountListRequest(
        @JsonProperty("Header") SsafyCommonHeader header
) {
}
