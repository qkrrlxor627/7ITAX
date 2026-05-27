package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyCreditCardTransactionResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") TransactionRec rec
) implements SsafyApiResponse {
    public record TransactionRec(
            @JsonProperty("transactionUniqueNo") Long transactionUniqueNo,
            @JsonProperty("cardNo") String cardNo,
            @JsonProperty("merchantId") Long merchantId,
            @JsonProperty("merchantName") String merchantName,
            @JsonProperty("categoryId") String categoryId,
            @JsonProperty("categoryName") String categoryName,
            @JsonProperty("paymentBalance") Long paymentBalance,
            @JsonProperty("transactionDate") String transactionDate,
            @JsonProperty("transactionTime") String transactionTime,
            @JsonProperty("status") String status
    ) {
    }
}
