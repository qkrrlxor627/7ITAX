package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyCreateAccountResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") AccountRec rec
) implements SsafyApiResponse {
    public record AccountRec(
            @JsonProperty("bankCode") String bankCode,
            @JsonProperty("accountNo") String accountNo
    ) {
    }
}
