package com.ssafy.tax7i.fcm.controller;

import com.ssafy.tax7i.fcm.dto.FcmTokenRequest;
import com.ssafy.tax7i.fcm.service.FcmService;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmService fcmService;

    @PostMapping("/token")
    public ResponseEntity<SuccessResponse<Void>> saveToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenRequest request) {
        fcmService.saveToken(userId, request.token(), request.deviceInfo());
        return ResponseEntity.ok(SuccessResponse.ok("FCM 토큰이 등록되었습니다."));
    }

    @DeleteMapping("/token")
    public ResponseEntity<SuccessResponse<Void>> removeToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenRequest request) {
        fcmService.removeToken(userId, request.token());
        return ResponseEntity.ok(SuccessResponse.ok("FCM 토큰이 삭제되었습니다."));
    }
}
