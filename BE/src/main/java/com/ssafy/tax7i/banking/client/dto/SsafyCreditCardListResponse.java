package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SsafyCreditCardListResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") List<CreditCardRec> rec
) implements SsafyApiResponse {
    public record CreditCardRec(
            @JsonProperty("cardNo") String cardNo,
            @JsonProperty("cvc") String cvc,
            @JsonProperty("cardUniqueNo") String cardUniqueNo,
            @JsonProperty("cardIssuerCode") String cardIssuerCode,
            @JsonProperty("cardIssuerName") String cardIssuerName,
            @JsonProperty("cardName") String cardName,
            @JsonProperty("baselinePerformance") String baselinePerformance,
            @JsonProperty("maxBenefitLimit") String maxBenefitLimit,
            @JsonProperty("cardDescription") String cardDescription,
            @JsonProperty("cardExpiryDate") String cardExpiryDate,
            @JsonProperty("withdrawalAccountNo") String withdrawalAccountNo,
            @JsonProperty("withdrawalDate") String withdrawalDate
    ) {
    }
}
