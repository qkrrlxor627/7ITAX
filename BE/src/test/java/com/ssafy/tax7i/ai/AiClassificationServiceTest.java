package com.ssafy.tax7i.ai;

import com.ssafy.tax7i.ai.client.AiServiceClient;
import com.ssafy.tax7i.ai.client.dto.AiClassifyResponse;
import com.ssafy.tax7i.ai.mapper.AiCategoryMapper;
import com.ssafy.tax7i.ai.service.AiClassificationService;
import com.ssafy.tax7i.classification.dto.ClassificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AiClassificationServiceTest {

    @Mock
    private AiServiceClient aiClient;

    @Spy
    private AiCategoryMapper categoryMapper = new AiCategoryMapper();

    @InjectMocks
    private AiClassificationService service;

    @Test
    @DisplayName("AI 분류 성공 → RECOMMENDED 결과 반환")
    void AI_분류_성공() {
        AiClassifyResponse aiResponse = new AiClassifyResponse(
                "접대비", 0.85f, "local_model", "거래처 식사", "소득세법§35");
        given(aiClient.classifyTransactionSafe("스타벅스")).willReturn(Optional.of(aiResponse));

        Optional<ClassificationResult> result = service.classify("스타벅스");

        assertThat(result).isPresent();
        assertThat(result.get().confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
        assertThat(result.get().taxCategory()).isEqualTo("접대비");
        assertThat(result.get().confidenceScore()).isEqualTo(85);
        assertThat(result.get().legalBasis()).isEqualTo("소득세법§35");
    }

    @Test
    @DisplayName("AI confidence 89 초과 시 89로 캡")
    void AI_confidence_캡() {
        AiClassifyResponse aiResponse = new AiClassifyResponse(
                "소모품비", 0.95f, "local_model", "사무용품", null);
        given(aiClient.classifyTransactionSafe("다이소")).willReturn(Optional.of(aiResponse));

        Optional<ClassificationResult> result = service.classify("다이소");

        assertThat(result).isPresent();
        assertThat(result.get().confidenceScore()).isEqualTo(89);
        assertThat(result.get().confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
    }

    @Test
    @DisplayName("AI 서비스 장애 → Optional.empty()")
    void AI_장애_graceful() {
        given(aiClient.classifyTransactionSafe("테스트")).willReturn(Optional.empty());

        Optional<ClassificationResult> result = service.classify("테스트");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("AI 미매핑 카테고리 → 기타 경비로 폴백")
    void AI_미매핑_카테고리() {
        AiClassifyResponse aiResponse = new AiClassifyResponse(
                "알수없는세목", 0.7f, "llm_fallback", "", "");
        given(aiClient.classifyTransactionSafe("미분류가맹점")).willReturn(Optional.of(aiResponse));

        Optional<ClassificationResult> result = service.classify("미분류가맹점");

        assertThat(result).isPresent();
        assertThat(result.get().taxCategory()).isEqualTo("기타 경비");
    }

    @Test
    @DisplayName("AI reason 비어있으면 기본 remark 생성")
    void AI_빈_reason_기본값() {
        AiClassifyResponse aiResponse = new AiClassifyResponse(
                "급료", 0.8f, "local_model", "", "");
        given(aiClient.classifyTransactionSafe("급여이체")).willReturn(Optional.of(aiResponse));

        Optional<ClassificationResult> result = service.classify("급여이체");

        assertThat(result).isPresent();
        assertThat(result.get().remark()).contains("AI 분류 결과");
    }
}
