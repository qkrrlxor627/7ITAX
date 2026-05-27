package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SsafyCreditCardProductListResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") List<CreditCardProductRec> rec
) implements SsafyApiResponse {
    public record CreditCardProductRec(
            @JsonProperty("cardUniqueNo") String cardUniqueNo,
            @JsonProperty("cardIssuerCode") String cardIssuerCode,
            @JsonProperty("cardIssuerName") String cardIssuerName,
            @JsonProperty("cardName") String cardName,
            @JsonProperty("baselinePerformance") String baselinePerformance,
            @JsonProperty("maxBenefitLimit") String maxBenefitLimit,
            @JsonProperty("cardDescription") String cardDescription
    ) {
    }
}
