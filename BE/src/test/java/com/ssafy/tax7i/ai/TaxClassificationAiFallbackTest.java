package com.ssafy.tax7i.ai;

import com.ssafy.tax7i.ai.service.AiClassificationService;
import com.ssafy.tax7i.classification.dto.ClassificationRequest;
import com.ssafy.tax7i.classification.dto.ClassificationResult;
import com.ssafy.tax7i.classification.entity.MccTaxRule;
import com.ssafy.tax7i.classification.entity.Merchant;
import com.ssafy.tax7i.classification.repository.MerchantKeywordMappingRepository;
import com.ssafy.tax7i.classification.repository.MerchantRepository;
import com.ssafy.tax7i.classification.repository.TaxLimitRepository;
import com.ssafy.tax7i.classification.service.ClassificationCacheService;
import com.ssafy.tax7i.classification.service.EntertainmentLimitService;
import com.ssafy.tax7i.classification.service.TaxClassificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TaxClassificationAiFallbackTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private ClassificationCacheService classificationCacheService;
    @Mock private MerchantKeywordMappingRepository keywordMappingRepository;
    @Mock private EntertainmentLimitService entertainmentLimitService;
    @Mock private TaxLimitRepository taxLimitRepository;
    @Mock private AiClassificationService aiClassificationService;
    @Mock private com.ssafy.tax7i.tax.service.TaxParameterService taxParameterService;
    @Mock private com.ssafy.tax7i.classification.service.CategoryLearningService categoryLearningService;

    @InjectMocks
    private TaxClassificationService service;

    @Test
    @DisplayName("MCC 결정 실패 + AI 분류 성공 → AI RECOMMENDED 반환")
    void MCC_없음_AI_성공() {
        ClassificationRequest request = new ClassificationRequest(
                "알수없는가맹점", null, 50000L, null, null, 1L);

        // MCC 결정 실패 (가맹점명으로도 못 찾음)
        given(classificationCacheService.getMerchantByName("알수없는가맹점")).willReturn(Optional.empty());
        given(merchantRepository.findByMerchantNameContainedIn(anyString())).willReturn(Collections.emptyList());

        // AI 분류 성공
        ClassificationResult aiResult = ClassificationResult.recommended(
                "소모품비", "확인필요", null, "AI 분류 결과", 75);
        given(aiClassificationService.classify("알수없는가맹점")).willReturn(Optional.of(aiResult));

        ClassificationResult result = service.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
        assertThat(result.taxCategory()).isEqualTo("소모품비");
        then(aiClassificationService).should().classify("알수없는가맹점");
    }

    @Test
    @DisplayName("MCC 결정 실패 + AI 장애 → 기존 NEEDS_CONFIRMATION 폴백")
    void MCC_없음_AI_실패() {
        ClassificationRequest request = new ClassificationRequest(
                "알수없는가맹점", null, 50000L, null, null, 1L);

        given(classificationCacheService.getMerchantByName("알수없는가맹점")).willReturn(Optional.empty());
        given(merchantRepository.findByMerchantNameContainedIn(anyString())).willReturn(Collections.emptyList());
        given(aiClassificationService.classify("알수없는가맹점")).willReturn(Optional.empty());

        ClassificationResult result = service.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.NEEDS_CONFIRMATION);
        assertThat(result.taxCategory()).isEqualTo("기타 경비");
    }

    @Test
    @DisplayName("MCC 룰 없음 + AI 분류 성공 → AI RECOMMENDED 반환")
    void 룰_없음_AI_성공() {
        ClassificationRequest request = new ClassificationRequest(
                "테스트가맹점", "9999", 50000L, null, null, 1L);

        given(classificationCacheService.getMccRules("9999")).willReturn(Collections.emptyList());

        ClassificationResult aiResult = ClassificationResult.recommended(
                "지급수수료", "확인필요", null, "AI 분류 결과", 70);
        given(aiClassificationService.classify("테스트가맹점")).willReturn(Optional.of(aiResult));

        ClassificationResult result = service.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
        assertThat(result.taxCategory()).isEqualTo("지급수수료");
    }

    @Test
    @DisplayName("Tier A 매칭 성공 → AI 호출 안 함")
    void TierA_성공시_AI_미호출() {
        ClassificationRequest request = new ClassificationRequest(
                "교보문고", "5942", 30000L, null, null, 1L);

        MccTaxRule tierARule = createTierARule("5942", "도서인쇄비", 95);
        given(classificationCacheService.getMccRules("5942")).willReturn(List.of(tierARule));

        ClassificationResult result = service.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.CONFIRMED);
        then(aiClassificationService).should(never()).classify(any());
    }

    private MccTaxRule createTierARule(String mcc, String category, int confidence) {
        try {
            var constructor = MccTaxRule.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            MccTaxRule rule = constructor.newInstance();
            setField(rule, "mcc", mcc);
            setField(rule, "tier", "A");
            setField(rule, "conditionExpr", "MCC_EXACT");
            setField(rule, "taxCategory", category);
            setField(rule, "vatDeductible", "공제");
            setField(rule, "confidence", confidence);
            setField(rule, "requiresUserConfirm", false);
            return rule;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
