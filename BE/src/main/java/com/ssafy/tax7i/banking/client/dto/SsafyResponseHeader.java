package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyResponseHeader(
        @JsonProperty("responseCode") String responseCode,
        @JsonProperty("responseMessage") String responseMessage,
        @JsonProperty("apiName") String apiName,
        @JsonProperty("transmissionDate") String transmissionDate,
        @JsonProperty("transmissionTime") String transmissionTime,
        @JsonProperty("institutionCode") String institutionCode,
        @JsonProperty("apiKey") String apiKey,
        @JsonProperty("apiServiceCode") String apiServiceCode,
        @JsonProperty("institutionTransactionUniqueNo") String institutionTransactionUniqueNo
) {
    public boolean isSuccess() {
        return "H0000".equals(responseCode);
    }
}
