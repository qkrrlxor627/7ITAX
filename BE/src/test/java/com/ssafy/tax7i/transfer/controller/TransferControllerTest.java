package com.ssafy.tax7i.transfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.config.TestSecurityConfig;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.transfer.dto.P2pTransferRequest;
import com.ssafy.tax7i.transfer.dto.TransferResponse;
import com.ssafy.tax7i.transfer.dto.WithdrawRequest;
import com.ssafy.tax7i.transfer.entity.TransferStatus;
import com.ssafy.tax7i.transfer.entity.TransferType;
import com.ssafy.tax7i.transfer.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class TransferControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TransferService transferService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── POST /api/transfers/p2p ─────────────

    @Test
    void p2pTransfer_200() throws Exception {
        TransferResponse response = new TransferResponse(
                1L, TransferType.P2P, 1L, 2L, 50000L,
                TransferStatus.COMPLETED, "P2P 송금", null, LocalDateTime.now());
        given(transferService.p2pTransfer(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/transfers/p2p")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "senderCardId", 1,
                                "receiverUserId", 2,
                                "amount", 50000,
                                "description", "P2P 송금"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.transferType").value("P2P"))
                .andExpect(jsonPath("$.data.amount").value(50000));
    }

    @Test
    void p2pTransfer_필수값누락_400() throws Exception {
        mockMvc.perform(post("/api/transfers/p2p")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 50000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    @Test
    void p2pTransfer_자기송금_400() throws Exception {
        given(transferService.p2pTransfer(any(), any()))
                .willThrow(new BusinessException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED));

        mockMvc.perform(post("/api/transfers/p2p")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "senderCardId", 1,
                                "receiverUserId", 1,
                                "amount", 50000
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SELF_TRANSFER_NOT_ALLOWED"));
    }

    // ───────────── POST /api/transfers/withdraw ─────────────

    @Test
    void withdraw_200() throws Exception {
        TransferResponse response = new TransferResponse(
                2L, TransferType.WITHDRAW, 1L, null, 30000L,
                TransferStatus.COMPLETED, "출금", "1234567890", LocalDateTime.now());
        given(transferService.withdraw(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/transfers/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cardId", 1,
                                "targetAccountNo", "1234567890",
                                "amount", 30000,
                                "description", "출금"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.transferType").value("WITHDRAW"))
                .andExpect(jsonPath("$.data.amount").value(30000));
    }

    @Test
    void withdraw_필수값누락_400() throws Exception {
        mockMvc.perform(post("/api/transfers/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 30000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ───────────── GET /api/transfers ─────────────

    @Test
    void getTransfers_200() throws Exception {
        TransferResponse response = new TransferResponse(
                1L, TransferType.P2P, 1L, 2L, 50000L,
                TransferStatus.COMPLETED, "P2P 송금", null, LocalDateTime.now());
        Page<TransferResponse> page = new PageImpl<>(List.of(response));
        given(transferService.getTransfers(any(), any())).willReturn(page);

        mockMvc.perform(get("/api/transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ───────────── GET /api/transfers/{transferId} ─────────────

    @Test
    void getTransfer_200() throws Exception {
        TransferResponse response = new TransferResponse(
                1L, TransferType.P2P, 1L, 2L, 50000L,
                TransferStatus.COMPLETED, "P2P 송금", null, LocalDateTime.now());
        given(transferService.getTransfer(any(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/transfers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.amount").value(50000));
    }

    @Test
    void getTransfer_없는송금_404() throws Exception {
        given(transferService.getTransfer(any(), eq(99L)))
                .willThrow(new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));

        mockMvc.perform(get("/api/transfers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TRANSFER_NOT_FOUND"));
    }
}
