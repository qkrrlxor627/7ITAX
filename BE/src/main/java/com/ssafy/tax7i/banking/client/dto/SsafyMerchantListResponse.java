package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SsafyMerchantListResponse(
        @JsonProperty("Header") SsafyResponseHeader header,
        @JsonProperty("REC") List<MerchantRec> rec
) implements SsafyApiResponse {
    public record MerchantRec(
            @JsonProperty("merchantId") Long merchantId,
            @JsonProperty("merchantName") String merchantName,
            @JsonProperty("categoryId") String categoryId,
            @JsonProperty("categoryName") String categoryName
    ) {
    }
}
