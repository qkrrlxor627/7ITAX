package com.ssafy.tax7i.card.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.card.dto.*;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.service.CardService;
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
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class CardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CardService cardService;
    @MockitoBean private com.ssafy.tax7i.sms.service.SmsOtpService smsOtpService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── POST /api/cards ─────────────

    @Test
    void createCard_201() throws Exception {
        CardResponse response = new CardResponse(1L, "사업용 카드", CardType.BUSINESS, "7890", false, "20290401", "4", LocalDateTime.now());
        given(cardService.createCard(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cardName", "사업용 카드",
                                "cardType", "BUSINESS",
                                "cardUniqueNo", "1003-xxx",
                                "withdrawalAccountNo", "0123456789012345",
                                "withdrawalDate", "4",
                                "otpToken", "test-otp-token"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.cardName").value("사업용 카드"))
                .andExpect(jsonPath("$.data.cardType").value("BUSINESS"))
                .andExpect(jsonPath("$.data.last4Digits").value("7890"));
    }

    @Test
    void createCard_필수값누락_400() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cardName", "테스트"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ───────────── GET /api/cards ─────────────

    @Test
    void getCards_200() throws Exception {
        List<CardResponse> responses = List.of(
                new CardResponse(1L, "카드1", CardType.BUSINESS, "1234", true, "20290401", "4", LocalDateTime.now()),
                new CardResponse(2L, "카드2", CardType.PERSONAL, "5678", false, "20290501", "1", LocalDateTime.now()));
        given(cardService.getCards(any())).willReturn(responses);

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ───────────── GET /api/cards/{id} ─────────────

    @Test
    void getCard_200() throws Exception {
        CardResponse response = new CardResponse(1L, "사업용 카드", CardType.BUSINESS, "7890", true, "20290401", "4", LocalDateTime.now());
        given(cardService.getCard(any(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardName").value("사업용 카드"))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    void getCard_없는카드_404() throws Exception {
        given(cardService.getCard(any(), eq(99L)))
                .willThrow(new BusinessException(ErrorCode.CARD_NOT_FOUND));

        mockMvc.perform(get("/api/cards/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CARD_NOT_FOUND"));
    }

    // ───────────── PATCH /api/cards/{id}/default ─────────────

    @Test
    void setDefault_200() throws Exception {
        CardResponse response = new CardResponse(1L, "사업용 카드", CardType.BUSINESS, "7890", true, "20290401", "4", LocalDateTime.now());
        given(cardService.setDefaultCard(any(), eq(1L))).willReturn(response);

        mockMvc.perform(patch("/api/cards/1/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    // ───────────── DELETE /api/cards/{id} ─────────────

    @Test
    void deleteCard_200() throws Exception {
        doNothing().when(cardService).deleteCard(any(), eq(1L));

        mockMvc.perform(delete("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    // ───────────── POST /api/cards/{id}/payment ─────────────

    @Test
    void payment_200() throws Exception {
        CardPaymentResponse response = new CardPaymentResponse(1L, 1L, "스타벅스", "카페", 50000L, "20260320", "120000");
        given(cardService.payment(any(), eq(1L), any())).willReturn(response);

        mockMvc.perform(post("/api/cards/1/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "merchantId", 1,
                                "paymentBalance", 50000
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentBalance").value(50000))
                .andExpect(jsonPath("$.data.merchantName").value("스타벅스"));
    }

    // ───────────── GET /api/cards/{id}/transactions ─────────────

    @Test
    void getTransactions_200() throws Exception {
        CardTransactionResponse txResponse = new CardTransactionResponse(
                1L, "20260320", "120000", "스타벅스", "카페", 50000L, "COMPLETED");
        given(cardService.getTransactions(any(), eq(1L), eq("20260301"), eq("20260331")))
                .willReturn(List.of(txResponse));

        mockMvc.perform(get("/api/cards/1/transactions")
                        .param("startDate", "20260301")
                        .param("endDate", "20260331"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].paymentBalance").value(50000));
    }

    // ─────────── POST /api/cards/{id}/activate ───────────

    @Test
    void activateCard_200() throws Exception {
        CardActivateResponse response = new CardActivateResponse(1L, "ACTIVE", LocalDateTime.now());
        given(cardService.activateCard(any(), eq(1L), any())).willReturn(response);

        mockMvc.perform(post("/api/cards/1/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("activationCode", "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void activateCard_없는카드_404() throws Exception {
        given(cardService.activateCard(any(), eq(99L), any()))
                .willThrow(new BusinessException(ErrorCode.CARD_NOT_FOUND));

        mockMvc.perform(post("/api/cards/99/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("activationCode", "1234"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CARD_NOT_FOUND"));
    }

    // ─────────── PATCH /api/cards/{id}/purpose ───────────

    @Test
    void setCardPurpose_200() throws Exception {
        CardPurposeResponse response = new CardPurposeResponse(1L, "BUSINESS", LocalDateTime.now());
        given(cardService.setCardPurpose(any(), eq(1L), any())).willReturn(response);

        mockMvc.perform(patch("/api/cards/1/purpose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("defaultPurpose", "BUSINESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.defaultPurpose").value("BUSINESS"));
    }

    @Test
    void setCardPurpose_필수값누락_400() throws Exception {
        mockMvc.perform(patch("/api/cards/1/purpose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }
}
