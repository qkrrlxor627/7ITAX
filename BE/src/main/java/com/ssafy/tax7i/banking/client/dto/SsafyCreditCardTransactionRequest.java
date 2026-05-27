package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyCreditCardTransactionRequest(
        @JsonProperty("Header") SsafyCommonHeader header,
        @JsonProperty("cardNo") String cardNo,
        @JsonProperty("cvc") String cvc,
        @JsonProperty("merchantId") Long merchantId,
        @JsonProperty("paymentBalance") Long paymentBalance
) {
}
