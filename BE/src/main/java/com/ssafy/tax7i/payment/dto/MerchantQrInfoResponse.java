package com.ssafy.tax7i.payment.dto;

import com.ssafy.tax7i.payment.entity.PaymentPurpose;

public record MerchantQrInfoResponse(
        String token,
        Long amount,
        String merchantName,
        String merchantCategoryCode,
        PaymentPurpose purpose
) {}
