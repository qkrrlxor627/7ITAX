package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SsafyAccountListResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") List<AccountRec> rec
) implements SsafyApiResponse {
    public record AccountRec(
            @JsonProperty("bankCode") String bankCode,
            @JsonProperty("accountNo") String accountNo,
            @JsonProperty("accountName") String accountName,
            @JsonProperty("accountBalance") String accountBalance
    ) {
    }
}
