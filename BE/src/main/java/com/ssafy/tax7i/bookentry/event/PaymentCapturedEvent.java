package com.ssafy.tax7i.bookentry.event;

import java.time.LocalDateTime;

/**
 * 결제 확정(capture) 시 발행되는 이벤트.
 * 사업자 카드 결제(purpose=BUSINESS)일 때만 장부를 자동 생성하기 위해 사용.
 */
public record PaymentCapturedEvent(
        Long paymentId,
        Long userId,
        Long amount,
        String merchantName,
        String merchantCategoryCode,
        String purpose,              // BUSINESS: 장부 생성 대상 / PERSONAL: 스킵
        LocalDateTime capturedAt
) {
}
