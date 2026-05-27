package com.ssafy.tax7i.auth.controller;

import com.ssafy.tax7i.auth.dto.*;
import com.ssafy.tax7i.auth.service.AuthService;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.test-login.enabled:false}")
    private boolean testLoginEnabled;

    @PostMapping("/verify-identity")
    public ResponseEntity<SuccessResponse<IdentityVerifyResponse>> verifyIdentity(
            @Valid @RequestBody IdentityVerifyRequest request) {
        IdentityVerifyResponse response = authService.verifyIdentity(request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/setup-pin")
    public ResponseEntity<SuccessResponse<LoginResponse>> setupPin(
            @RequestHeader("X-Verify-Token") String verifyToken,
            @Valid @RequestBody PinSetupRequest request) {
        LoginResponse response = authService.setupPin(verifyToken, request.pin());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<LoginResponse>> login(
            @Valid @RequestBody PinLoginRequest request) {
        LoginResponse response = authService.loginWithPin(request.phoneNumber(), request.pin());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/reissue")
    public ResponseEntity<SuccessResponse<LoginResponse>> reissue(
            @Valid @RequestBody TokenReissueRequest request) {
        LoginResponse response = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Void>> logout(
            @RequestHeader("Authorization") String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        String accessToken = authorization.substring(7);
        authService.logout(accessToken);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // 테스트 전용 — app.test-login.enabled 프로퍼티로 보호 (기본값 false, local 프로파일에서만 true)
    @PostMapping("/test-login")
    public ResponseEntity<SuccessResponse<LoginResponse>> testLogin(
            @RequestParam(value = "email", required = false) String email) {
        if (!testLoginEnabled) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "테스트 로그인이 비활성화되어 있습니다.");
        }
        LoginResponse response = authService.testLogin(email);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
