package com.ssafy.tax7i.ai.service;

import com.ssafy.tax7i.ai.client.AiServiceClient;
import com.ssafy.tax7i.ai.client.dto.AiClassifyResponse;
import com.ssafy.tax7i.ai.mapper.AiCategoryMapper;
import com.ssafy.tax7i.classification.dto.ClassificationResult;
import com.ssafy.tax7i.classification.entity.TaxCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * AI 기반 세목 분류 서비스.
 * MCC 규칙 분류 실패 시 폴백으로 사용된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClassificationService {

    private final AiServiceClient aiClient;
    private final AiCategoryMapper categoryMapper;

    /**
     * AI 세목 분류를 시도한다.
     * AI 서비스 장애 시 Optional.empty()를 반환하여 기존 폴백으로 처리한다.
     *
     * @param merchantName 가맹점명 (AI에 description으로 전달)
     * @return AI 분류 결과 (RECOMMENDED), 실패 시 empty
     */
    public Optional<ClassificationResult> classify(String merchantName) {
        Optional<AiClassifyResponse> aiResult = aiClient.classifyTransactionSafe(merchantName);

        if (aiResult.isEmpty()) {
            return Optional.empty();
        }

        AiClassifyResponse response = aiResult.get();
        TaxCategory mapped = categoryMapper.map(response.category());

        // AI confidence(0~1) → BE score(0~100), 최대 89로 캡 (CONFIRMED 방지)
        int score = Math.min(Math.round(response.confidence() * 100), 89);

        String vatDeductible = "확인필요";
        String legalBasis = response.legalBasis();
        String remark = response.reason();

        if (remark == null || remark.isBlank()) {
            remark = "AI 분류 결과 (confidence: " + response.confidence() + ")";
        }

        log.info("AI 세목 분류: merchant={}, aiCategory={}, mappedCategory={}, score={}",
                merchantName, response.category(), mapped.getName(), score);

        return Optional.of(ClassificationResult.recommended(
                mapped.getName(), vatDeductible, legalBasis, remark, score));
    }
}
