package com.ssafy.tax7i.classification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.classification.dto.ClassificationResult;
import com.ssafy.tax7i.classification.service.TaxClassificationService;
import com.ssafy.tax7i.config.TestSecurityConfig;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClassificationController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class ClassificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TaxClassificationService classificationService;
    @MockitoBean private com.ssafy.tax7i.ai.service.AiRateLimiter aiRateLimiter;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── POST /api/classification ─────────────

    @Test
    void classify_200_성공() throws Exception {
        ClassificationResult result = ClassificationResult.confirmed(
                "소모품비", "공제", "소득세법§27", "100만원 미만 소모품", 95);
        given(classificationService.classify(any())).willReturn(result);

        mockMvc.perform(post("/api/classification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "merchantName", "쿠팡",
                                "mcc", "5999",
                                "amount", 50000
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.confidence").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.taxCategory").value("소모품비"))
                .andExpect(jsonPath("$.data.confidenceScore").value(95));
    }

    @Test
    void classify_가맹점명누락_400() throws Exception {
        mockMvc.perform(post("/api/classification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mcc", "5999",
                                "amount", 50000
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }
}
