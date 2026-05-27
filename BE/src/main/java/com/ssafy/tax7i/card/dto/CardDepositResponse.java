package com.ssafy.tax7i.card.dto;

public record CardDepositResponse(
        Long cardId,
        Long depositAmount,
        Long balance
) {}
