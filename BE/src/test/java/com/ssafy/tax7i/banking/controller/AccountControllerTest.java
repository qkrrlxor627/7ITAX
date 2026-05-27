package com.ssafy.tax7i.banking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.banking.dto.AccountCreateRequest;
import com.ssafy.tax7i.banking.dto.AccountResponse;
import com.ssafy.tax7i.banking.dto.BalanceResponse;
import com.ssafy.tax7i.banking.entity.AccountType;
import com.ssafy.tax7i.banking.service.AccountService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AccountService accountService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ─────────── POST /api/banking/accounts ───────────

    @Test
    void createAccount_201_계좌개설() throws Exception {
        AccountResponse response = new AccountResponse(
                1L, "BUSINESS", "088", "110-123-456789", "사업용 계좌", 0L, "ACTIVE", LocalDateTime.now());
        given(accountService.createAccount(any(), any(AccountCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/banking/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "accountType", "BUSINESS",
                                "bankCode", "088-1-XXXXXXXXX"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.accountType").value("BUSINESS"))
                .andExpect(jsonPath("$.data.bankCode").value("088"))
                .andExpect(jsonPath("$.data.balance").value(0));
    }

    @Test
    void createAccount_필수값누락_400() throws Exception {
        // bankCode 누락
        mockMvc.perform(post("/api/banking/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "accountType", "PERSONAL"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ─────────── GET /api/banking/accounts ───────────

    @Test
    void getAccounts_200_전체목록() throws Exception {
        List<AccountResponse> responses = List.of(
                new AccountResponse(1L, "PERSONAL", "088", "110-111-111111", null, 50000L, "ACTIVE", LocalDateTime.now()),
                new AccountResponse(2L, "BUSINESS", "088", "110-222-222222", "사업 계좌", 100000L, "ACTIVE", LocalDateTime.now())
        );
        given(accountService.getAccounts(any(), eq(null))).willReturn(responses);

        mockMvc.perform(get("/api/banking/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getAccounts_200_타입필터() throws Exception {
        List<AccountResponse> responses = List.of(
                new AccountResponse(1L, "PERSONAL", "088", "110-111-111111", null, 50000L, "ACTIVE", LocalDateTime.now())
        );
        given(accountService.getAccounts(any(), eq("PERSONAL"))).willReturn(responses);

        mockMvc.perform(get("/api/banking/accounts")
                        .param("accountType", "PERSONAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].accountType").value("PERSONAL"));
    }

    // ─────────── GET /api/banking/accounts/{accountId} ───────────

    @Test
    void getAccount_200_상세조회() throws Exception {
        AccountResponse response = new AccountResponse(
                1L, "BUSINESS", "088", "110-123-456789", "사업용 계좌", 75000L, "ACTIVE", LocalDateTime.now());
        given(accountService.getAccount(any(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/banking/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.balance").value(75000))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void getAccount_404_없는계좌() throws Exception {
        given(accountService.getAccount(any(), eq(99L)))
                .willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        mockMvc.perform(get("/api/banking/accounts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"));
    }

    // ─────────── GET /api/banking/accounts/{accountId}/balance ───────────

    @Test
    void getBalance_200() throws Exception {
        BalanceResponse response = BalanceResponse.of(1L, 123456L);
        given(accountService.getBalance(any(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/banking/accounts/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.accountId").value(1))
                .andExpect(jsonPath("$.data.balance").value(123456))
                .andExpect(jsonPath("$.data.availableBalance").value(123456))
                .andExpect(jsonPath("$.data.pendingAmount").value(0));
    }
}
