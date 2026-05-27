package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SsafyCreditCardTransactionListResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") List<TransactionRec> rec
) implements SsafyApiResponse {
    public record TransactionRec(
            @JsonProperty("transactionUniqueNo") Long transactionUniqueNo,
            @JsonProperty("transactionDate") String transactionDate,
            @JsonProperty("transactionTime") String transactionTime,
            @JsonProperty("cardNo") String cardNo,
            @JsonProperty("merchantId") Long merchantId,
            @JsonProperty("merchantName") String merchantName,
            @JsonProperty("categoryId") String categoryId,
            @JsonProperty("categoryName") String categoryName,
            @JsonProperty("paymentBalance") Long paymentBalance,
            @JsonProperty("status") String status
    ) {
    }
}
