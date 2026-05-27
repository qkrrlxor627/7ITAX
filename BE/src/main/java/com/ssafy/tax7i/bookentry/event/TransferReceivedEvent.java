package com.ssafy.tax7i.bookentry.event;

import java.time.LocalDateTime;

/**
 * P2P 이체 수신 시 발행되는 이벤트.
 * 수신자(사업자)에게 매출 장부를 자동 생성하기 위해 사용.
 */
public record TransferReceivedEvent(
        Long transferId,
        Long receiverUserId,
        Long amount,
        String senderName,
        String description,
        LocalDateTime transferredAt
) {
}
