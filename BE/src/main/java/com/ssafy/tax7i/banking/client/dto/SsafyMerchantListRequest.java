package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyMerchantListRequest(
        @JsonProperty("Header") SsafyCommonHeader header
) {
}
