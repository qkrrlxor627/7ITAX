package com.ssafy.tax7i.sms.controller;

import com.ssafy.tax7i.sms.dto.OtpSendResponse;
import com.ssafy.tax7i.sms.dto.OtpVerifyRequest;
import com.ssafy.tax7i.sms.dto.OtpVerifyResponse;
import com.ssafy.tax7i.sms.service.SmsOtpService;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsOtpService smsOtpService;

    /**
     * SMS OTP 발송 요청
     */
    @PostMapping("/send")
    public ResponseEntity<SuccessResponse<OtpSendResponse>> sendOtp(
            @AuthenticationPrincipal Long userId) {
        OtpSendResponse response = smsOtpService.sendOtp(userId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * SMS OTP 검증
     */
    @PostMapping("/verify")
    public ResponseEntity<SuccessResponse<OtpVerifyResponse>> verifyOtp(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OtpVerifyRequest request) {
        OtpVerifyResponse response = smsOtpService.verifyOtp(userId, request.otpCode());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
