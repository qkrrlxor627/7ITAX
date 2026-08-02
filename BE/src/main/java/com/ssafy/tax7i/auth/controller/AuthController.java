package com.ssafy.tax7i.auth.controller;

import com.ssafy.tax7i.auth.dto.AdditionalAuthLoginRequest;
import com.ssafy.tax7i.auth.dto.IdentityVerifyRequest;
import com.ssafy.tax7i.auth.dto.IdentityVerifyResponse;
import com.ssafy.tax7i.auth.dto.LoginResponse;
import com.ssafy.tax7i.auth.dto.PinLoginRequest;
import com.ssafy.tax7i.auth.dto.PinSetupRequest;
import com.ssafy.tax7i.auth.dto.TokenReissueRequest;
import com.ssafy.tax7i.auth.service.AuthService;
import com.ssafy.tax7i.global.audit.Auditable;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.test-login.enabled:false}")
    private boolean testLoginEnabled;

    @Auditable(action = "IDENTITY_VERIFY")
    @PostMapping("/verify-identity")
    public ResponseEntity<SuccessResponse<IdentityVerifyResponse>> verifyIdentity(
            @Valid @RequestBody IdentityVerifyRequest request) {
        IdentityVerifyResponse response = authService.verifyIdentity(request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @Auditable(action = "PIN_SETUP")
    @PostMapping("/setup-pin")
    public ResponseEntity<SuccessResponse<LoginResponse>> setupPin(
            @RequestHeader("X-Verify-Token") String verifyToken,
            @Valid @RequestBody PinSetupRequest request) {
        LoginResponse response = authService.setupPin(
                verifyToken,
                request.pin(),
                request.deviceId(),
                request.consents()
        );
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @Auditable(action = "LOGIN")
    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<LoginResponse>> login(
            @Valid @RequestBody PinLoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.loginWithPin(
                request.phoneNumber(),
                request.pin(),
                request.deviceId(),
                request.deviceName(),
                clientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @Auditable(action = "LOGIN_ADDITIONAL_AUTH")
    @PostMapping("/login/additional-auth")
    public ResponseEntity<SuccessResponse<LoginResponse>> completeAdditionalAuth(
            @Valid @RequestBody AdditionalAuthLoginRequest request) {
        LoginResponse response = authService.completeAdditionalAuth(
                request.additionalAuthToken(),
                request.otpCode()
        );
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @Auditable(action = "TOKEN_REISSUE")
    @PostMapping("/reissue")
    public ResponseEntity<SuccessResponse<LoginResponse>> reissue(
            @Valid @RequestBody TokenReissueRequest request) {
        LoginResponse response = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @Auditable(action = "LOGOUT")
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

    @PostMapping("/test-login")
    public ResponseEntity<SuccessResponse<LoginResponse>> testLogin(
            @RequestParam(value = "email", required = false) String email) {
        if (!testLoginEnabled) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "테스트 로그인이 비활성화되어 있습니다.");
        }
        LoginResponse response = authService.testLogin(email);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor;
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
