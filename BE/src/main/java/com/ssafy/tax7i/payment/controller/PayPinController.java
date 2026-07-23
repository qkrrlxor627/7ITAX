package com.ssafy.tax7i.payment.controller;

import com.ssafy.tax7i.global.response.SuccessResponse;
import com.ssafy.tax7i.payment.dto.PayPinRequest;
import com.ssafy.tax7i.payment.dto.PayPinStatusResponse;
import com.ssafy.tax7i.payment.service.PayPinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pay/pin")
@RequiredArgsConstructor
public class PayPinController {

    private final PayPinService payPinService;

    /** 결제 비밀번호 설정/변경. */
    @PostMapping
    public ResponseEntity<SuccessResponse<Void>> setPayPin(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PayPinRequest request) {
        payPinService.setPayPin(userId, request.pin());
        return ResponseEntity.ok(SuccessResponse.ok("결제 비밀번호가 설정되었습니다."));
    }

    /** 결제 비밀번호 검증. */
    @PostMapping("/verify")
    public ResponseEntity<SuccessResponse<Void>> verifyPayPin(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PayPinRequest request) {
        payPinService.verifyPayPin(userId, request.pin());
        return ResponseEntity.ok(SuccessResponse.ok("결제 비밀번호가 확인되었습니다."));
    }

    /** 결제 비밀번호 설정 여부 조회. */
    @GetMapping("/status")
    public ResponseEntity<SuccessResponse<PayPinStatusResponse>> getStatus(
            @AuthenticationPrincipal Long userId) {
        boolean registered = payPinService.hasPayPin(userId);
        return ResponseEntity.ok(SuccessResponse.of(new PayPinStatusResponse(registered)));
    }
}
