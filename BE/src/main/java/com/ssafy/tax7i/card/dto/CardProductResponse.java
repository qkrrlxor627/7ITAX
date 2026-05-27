package com.ssafy.tax7i.card.dto;

import com.ssafy.tax7i.banking.client.dto.SsafyCreditCardProductListResponse;

public record CardProductResponse(
        String cardUniqueNo,
        String cardIssuerName,
        String cardName,
        String baselinePerformance,
        String maxBenefitLimit,
        String cardDescription
) {
    public static CardProductResponse from(SsafyCreditCardProductListResponse.CreditCardProductRec rec) {
        return new CardProductResponse(
                rec.cardUniqueNo(),
                rec.cardIssuerName(),
                rec.cardName(),
                rec.baselinePerformance(),
                rec.maxBenefitLimit(),
                rec.cardDescription()
        );
    }
}
