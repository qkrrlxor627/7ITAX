package com.ssafy.tax7i.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.config.TestSecurityConfig;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.payment.dto.*;
import com.ssafy.tax7i.payment.entity.PaymentMethod;
import com.ssafy.tax7i.payment.entity.PaymentPurpose;
import com.ssafy.tax7i.payment.entity.PaymentStatus;
import com.ssafy.tax7i.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PaymentService paymentService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── POST /api/payments/authorize ─────────────

    @Test
    void authorize_200_결제승인() throws Exception {
        PaymentAuthorizeResponse response = new PaymentAuthorizeResponse(
                1L, "AUTH1234", 1L, 10000L, PaymentStatus.AUTHORIZED, PaymentPurpose.BUSINESS, LocalDateTime.now());
        given(paymentService.authorize(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cardId", 1,
                                "amount", 10000,
                                "merchantId", 1,
                                "merchantName", "스타벅스",
                                "paymentMethod", "OFFLINE",
                                "purpose", "BUSINESS"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.data.amount").value(10000));
    }

    @Test
    void authorize_필수값누락_400() throws Exception {
        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 10000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    @Test
    void authorize_잔액부족_422() throws Exception {
        given(paymentService.authorize(any(), any()))
                .willThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE));

        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cardId", 1,
                                "amount", 10000,
                                "merchantId", 1,
                                "merchantName", "스타벅스",
                                "paymentMethod", "OFFLINE",
                                "purpose", "BUSINESS"
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"));
    }

    // ───────────── POST /api/payments/{id}/capture ─────────────

    @Test
    void capture_200_결제확정() throws Exception {
        PaymentCaptureResponse mockResponse = new PaymentCaptureResponse(
                1L, PaymentStatus.CAPTURED, 1L, 10000L, LocalDateTime.now());
        given(paymentService.capture(any(), eq(1L))).willReturn(mockResponse);

        mockMvc.perform(post("/api/payments/1/capture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CAPTURED"))
                .andExpect(jsonPath("$.data.amount").value(10000));
    }

    @Test
    void capture_결제없음_404() throws Exception {
        given(paymentService.capture(any(), eq(99L)))
                .willThrow(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        mockMvc.perform(post("/api/payments/99/capture"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PAYMENT_NOT_FOUND"));
    }

    // ───────────── POST /api/payments/{id}/cancel ─────────────

    @Test
    void cancel_200_결제취소() throws Exception {
        PaymentCancelResponse response = new PaymentCancelResponse(
                1L, PaymentStatus.CANCELLED, 10000L, 1L, LocalDateTime.now());
        given(paymentService.cancel(any(), eq(1L), any())).willReturn(response);

        mockMvc.perform(post("/api/payments/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("cancelAmount", 10000, "reason", "고객 요청"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAmount").value(10000));
    }

    @Test
    void cancel_사유없음_400() throws Exception {
        mockMvc.perform(post("/api/payments/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("cancelAmount", 10000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ───────────── GET /api/payments/{id} ─────────────

    @Test
    void getPayment_200_상세조회() throws Exception {
        PaymentDetailResponse response = new PaymentDetailResponse(
                1L, 1L, 15000L, "KRW", "스타벅스", "5812",
                PaymentMethod.OFFLINE, PaymentPurpose.BUSINESS, PaymentStatus.CAPTURED,
                "AUTH1234", null, null,
                LocalDateTime.now(), LocalDateTime.now(), null, LocalDateTime.now());
        given(paymentService.getPayment(any(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(15000))
                .andExpect(jsonPath("$.data.merchantName").value("스타벅스"));
    }

    @Test
    void getPayment_404_결제없음() throws Exception {
        given(paymentService.getPayment(any(), eq(99L)))
                .willThrow(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        mockMvc.perform(get("/api/payments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PAYMENT_NOT_FOUND"));
    }
}
