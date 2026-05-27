package com.ssafy.tax7i.payment.event;

import com.ssafy.tax7i.bookentry.event.PaymentCapturedEvent;
import com.ssafy.tax7i.fcm.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentNotificationListener {

    private final FcmService fcmService;

    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        String title = "결제 완료";
        String body = String.format("%s %,d원 결제가 완료되었습니다.",
                event.merchantName(), event.amount());
        Map<String, String> data = Map.of(
                "type", "payment",
                "paymentId", String.valueOf(event.paymentId()),
                "amount", String.valueOf(event.amount()),
                "merchantName", event.merchantName());
        fcmService.sendNotification(event.userId(), title, body, data);
    }

    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCancelled(PaymentCancelledEvent event) {
        String title = "결제 취소";
        String body = String.format("%s %,d원 결제가 취소되었습니다.",
                event.merchantName(), event.amount());
        Map<String, String> data = Map.of(
                "type", "payment_cancel",
                "paymentId", String.valueOf(event.paymentId()),
                "amount", String.valueOf(event.amount()),
                "merchantName", event.merchantName());
        fcmService.sendNotification(event.userId(), title, body, data);
    }

    @Recover
    public void recoverPaymentCaptured(Exception e, PaymentCapturedEvent event) {
        log.error("결제 완료 FCM 알림 최종 실패 (3회 재시도 후): paymentId={}, error={}",
                event.paymentId(), e.getMessage());
    }

    @Recover
    public void recoverPaymentCancelled(Exception e, PaymentCancelledEvent event) {
        log.error("결제 취소 FCM 알림 최종 실패 (3회 재시도 후): paymentId={}, error={}",
                event.paymentId(), e.getMessage());
    }
}
