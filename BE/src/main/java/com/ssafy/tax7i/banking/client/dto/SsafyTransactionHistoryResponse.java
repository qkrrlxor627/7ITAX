package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SsafyTransactionHistoryResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") List<TransactionRecord> rec
) implements SsafyApiResponse {
    public record TransactionRecord(
            @JsonProperty("transactionUniqueNo") String transactionUniqueNo,
            @JsonProperty("transactionDate") String transactionDate,
            @JsonProperty("transactionTime") String transactionTime,
            @JsonProperty("transactionType") String transactionType,
            @JsonProperty("transactionTypeName") String transactionTypeName,
            @JsonProperty("transactionAccountNo") String transactionAccountNo,
            @JsonProperty("transactionBalance") String transactionBalance,
            @JsonProperty("transactionAfterBalance") String transactionAfterBalance,
            @JsonProperty("transactionSummary") String transactionSummary,
            @JsonProperty("transactionMemo") String transactionMemo
    ) {}
}
