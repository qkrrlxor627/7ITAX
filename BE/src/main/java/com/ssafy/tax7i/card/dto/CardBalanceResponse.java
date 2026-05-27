package com.ssafy.tax7i.card.dto;

public record CardBalanceResponse(
        Long cardId,
        String cardName,
        Long balance
) {}
