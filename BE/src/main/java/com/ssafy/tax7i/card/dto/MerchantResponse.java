package com.ssafy.tax7i.card.dto;

import com.ssafy.tax7i.banking.client.dto.SsafyMerchantListResponse;

public record MerchantResponse(
        Long merchantId,
        String merchantName,
        String categoryId,
        String categoryName
) {
    public static MerchantResponse from(SsafyMerchantListResponse.MerchantRec rec) {
        return new MerchantResponse(rec.merchantId(), rec.merchantName(), rec.categoryId(), rec.categoryName());
    }
}
