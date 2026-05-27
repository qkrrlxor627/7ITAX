package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyCreateAccountRequest(
        @JsonProperty("Header") SsafyCommonHeader header,
        @JsonProperty("accountTypeUniqueNo") String accountTypeUniqueNo
) {
}
