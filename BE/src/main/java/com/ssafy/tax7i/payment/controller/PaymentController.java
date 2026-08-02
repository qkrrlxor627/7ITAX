package com.ssafy.tax7i.payment.controller;

import com.ssafy.tax7i.global.audit.Auditable;
import com.ssafy.tax7i.global.response.SuccessResponse;
import com.ssafy.tax7i.payment.dto.*;
import com.ssafy.tax7i.payment.entity.PaymentStatus;
import com.ssafy.tax7i.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Auditable(action = "PAYMENT_AUTHORIZE")
    @PostMapping("/authorize")
    public ResponseEntity<SuccessResponse<PaymentAuthorizeResponse>> authorize(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PaymentAuthorizeRequest request) {
        PaymentAuthorizeResponse response = paymentService.authorize(userId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @Auditable(action = "PAYMENT_CAPTURE")
    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<SuccessResponse<PaymentCaptureResponse>> capture(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId) {
        PaymentCaptureResponse response = paymentService.capture(userId, paymentId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @Auditable(action = "PAYMENT_CANCEL")
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<SuccessResponse<PaymentCancelResponse>> cancel(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentCancelRequest request) {
        PaymentCancelResponse response = paymentService.cancel(userId, paymentId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<SuccessResponse<PaymentDetailResponse>> getPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId) {
        PaymentDetailResponse response = paymentService.getPayment(userId, paymentId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<Page<PaymentDetailResponse>>> getPayments(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) PaymentStatus status,
            Pageable pageable) {
        Page<PaymentDetailResponse> response = paymentService.getPayments(userId, startDate, endDate, status, pageable);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @Auditable(action = "QR_PAYMENT")
    @PostMapping("/qr")
    public ResponseEntity<SuccessResponse<QrPaymentResponse>> processQrPayment(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody QrPaymentRequest request) {
        QrPaymentResponse response = paymentService.processQrPayment(userId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    // ───────────── QR 토큰 결제 (MPM) ─────────────

    @Auditable(action = "QR_TOKEN_CREATE")
    @PostMapping("/qr/token")
    public ResponseEntity<SuccessResponse<QrTokenCreateResponse>> createQrToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody QrTokenCreateRequest request) {
        QrTokenCreateResponse response = paymentService.createQrToken(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.of(response));
    }

    // 가맹점(수신자)용 — userId는 인증 강제 목적, 결제 소유자 검증은 하지 않음 (호출자 ≠ 결제 생성자)
    @GetMapping("/qr/token/{token}")
    public ResponseEntity<SuccessResponse<QrPaymentInfoResponse>> getQrPaymentInfo(
            @AuthenticationPrincipal Long userId,
            @PathVariable String token) {
        QrPaymentInfoResponse response = paymentService.getQrPaymentInfo(token);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    // 가맹점(수신자)용 — userId는 인증 강제 목적, 결제 소유자 검증은 하지 않음 (호출자 ≠ 결제 생성자)
    @Auditable(action = "QR_PAYMENT_CONFIRM")
    @PostMapping("/qr/token/{token}/confirm")
    public ResponseEntity<SuccessResponse<QrPaymentResponse>> confirmQrPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable String token) {
        QrPaymentResponse response = paymentService.confirmQrPayment(token);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/qr/token/{token}/status")
    public ResponseEntity<SuccessResponse<QrPaymentStatusResponse>> getQrPaymentStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable String token) {
        QrPaymentStatusResponse response = paymentService.getQrPaymentStatus(userId, token);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping(value = "/qr/token/{token}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeQrPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable String token) {
        return paymentService.subscribeQrPayment(userId, token);
    }

    // ───────────── 가맹점 QR 결제 (MPM) ─────────────

    @Auditable(action = "MERCHANT_QR_TOKEN_CREATE")
    @PostMapping("/qr/merchant-token")
    public ResponseEntity<SuccessResponse<MerchantQrTokenResponse>> createMerchantQrToken(
            @Valid @RequestBody MerchantQrCreateRequest request) {
        MerchantQrTokenResponse response = paymentService.createMerchantQrToken(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.of(response));
    }

    @GetMapping("/qr/merchant-token/{token}")
    public ResponseEntity<SuccessResponse<MerchantQrInfoResponse>> getMerchantQrInfo(
            @PathVariable String token) {
        MerchantQrInfoResponse response = paymentService.getMerchantQrInfo(token);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @Auditable(action = "MERCHANT_QR_PAY")
    @PostMapping("/qr/merchant-token/{token}/pay")
    public ResponseEntity<SuccessResponse<QrPaymentResponse>> payMerchantQr(
            @AuthenticationPrincipal Long userId,
            @PathVariable String token,
            @Valid @RequestBody MerchantQrPayRequest request) {
        QrPaymentResponse response = paymentService.payMerchantQr(userId, token, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping(value = "/qr/merchant-token/{token}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeMerchantQrPayment(@PathVariable String token) {
        return paymentService.subscribeMerchantQrPayment(token);
    }
}
