package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyCommonHeader(
        @JsonProperty("apiName") String apiName,
        @JsonProperty("transmissionDate") String transmissionDate,
        @JsonProperty("transmissionTime") String transmissionTime,
        @JsonProperty("institutionCode") String institutionCode,
        @JsonProperty("fintechAppNo") String fintechAppNo,
        @JsonProperty("apiServiceCode") String apiServiceCode,
        @JsonProperty("institutionTransactionUniqueNo") String institutionTransactionUniqueNo,
        @JsonProperty("apiKey") String apiKey,
        @JsonProperty("userKey") String userKey
) {
}
