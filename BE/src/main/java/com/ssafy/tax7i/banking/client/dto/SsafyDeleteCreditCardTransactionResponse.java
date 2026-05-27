package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyDeleteCreditCardTransactionResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") DeletedTransactionRec rec
) implements SsafyApiResponse {
    public record DeletedTransactionRec(
            @JsonProperty("transactionUniqueNo") Long transactionUniqueNo,
            @JsonProperty("status") String status
    ) {
    }
}
