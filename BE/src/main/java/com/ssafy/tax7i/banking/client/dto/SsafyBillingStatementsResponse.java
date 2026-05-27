package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SsafyBillingStatementsResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") List<BillingRec> rec
) implements SsafyApiResponse {
    public record BillingRec(
            @JsonProperty("billingMonth") String billingMonth,
            @JsonProperty("billingAmount") Long billingAmount,
            @JsonProperty("paidAmount") Long paidAmount,
            @JsonProperty("unpaidAmount") Long unpaidAmount,
            @JsonProperty("billingDate") String billingDate,
            @JsonProperty("paymentDueDate") String paymentDueDate,
            @JsonProperty("status") String status
    ) {
    }
}
