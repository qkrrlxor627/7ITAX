package com.ssafy.tax7i.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.auth.dto.IdentityVerifyResponse;
import com.ssafy.tax7i.auth.dto.LoginResponse;
import com.ssafy.tax7i.auth.service.AuthService;
import com.ssafy.tax7i.config.TestSecurityConfig;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── POST /api/auth/verify-identity ─────────────

    @Test
    void verifyIdentity_200_본인인증성공() throws Exception {
        given(authService.verifyIdentity(any()))
                .willReturn(new IdentityVerifyResponse(1L, true, true, "verify-token-uuid"));

        mockMvc.perform(post("/api/auth/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "홍길동",
                                "birthDate", "19900101",
                                "gender", "M",
                                "phoneNumber", "01012345678"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(jsonPath("$.data.requiresPinSetup").value(true))
                .andExpect(jsonPath("$.data.verifyToken").value("verify-token-uuid"));
    }

    @Test
    void verifyIdentity_필수값누락_400() throws Exception {
        mockMvc.perform(post("/api/auth/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "홍길동"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ───────────── POST /api/auth/setup-pin ─────────────

    @Test
    void setupPin_200_JWT발급() throws Exception {
        given(authService.setupPin(any(), any(), any(), any()))
                .willReturn(new LoginResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/setup-pin")
                        .header("X-Verify-Token", "valid-verify-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void setupPin_PIN형식오류_400() throws Exception {
        mockMvc.perform(post("/api/auth/setup-pin")
                        .header("X-Verify-Token", "valid-verify-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ───────────── POST /api/auth/login ─────────────

    @Test
    void login_200_JWT발급() throws Exception {
        given(authService.loginWithPin(eq("01012345678"), eq("123456"), any(), any(), any(), any()))
                .willReturn(new LoginResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phoneNumber", "01012345678",
                                "pin", "123456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void login_PIN불일치_401() throws Exception {
        given(authService.loginWithPin(eq("01012345678"), eq("000000"), any(), any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.PIN_INVALID));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phoneNumber", "01012345678",
                                "pin", "000000"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("PIN_INVALID"));
    }

    // ───────────── POST /api/auth/reissue ─────────────

    @Test
    void reissue_유효한refreshToken_200() throws Exception {
        given(authService.reissue("valid-rt"))
                .willReturn(new LoginResponse("new-at", "new-rt"));

        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "valid-rt"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-at"));
    }

    @Test
    void reissue_빈refreshToken_400() throws Exception {
        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ───────────── POST /api/auth/logout ─────────────

    @Test
    void logout_유효한Bearer헤더_200() throws Exception {
        willDoNothing().given(authService).logout("valid-at");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer valid-at"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void logout_Bearer없는헤더_401() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_INVALID"));
    }

    // ───────────── POST /api/auth/test-login ─────────────

    @Test
    void testLogin_200_기본() throws Exception {
        given(authService.testLogin(null))
                .willReturn(new LoginResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/test-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void testLogin_200_커스텀이메일() throws Exception {
        given(authService.testLogin("custom@test.com"))
                .willReturn(new LoginResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/test-login")
                        .param("email", "custom@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }
}
