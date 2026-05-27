package com.ssafy.tax7i.banking.client.dto;

public record SsafyTransferResult(
        SsafyWithdrawResponse withdrawResponse,
        SsafyDepositResponse depositResponse
) {}
