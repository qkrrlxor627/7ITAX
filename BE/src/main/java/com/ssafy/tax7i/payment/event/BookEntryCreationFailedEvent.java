package com.ssafy.tax7i.payment.event;

/**
 * 결제 후 장부 자동 생성이 실패했을 때 발행되는 이벤트.
 * 후속 배치 재처리 등의 보상 로직에서 활용한다.
 */
public record BookEntryCreationFailedEvent(Long paymentId) {
}
