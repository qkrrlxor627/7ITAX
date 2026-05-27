package com.ssafy.tax7i.classification.service;

import com.ssafy.tax7i.classification.dto.ClassificationRequest;
import com.ssafy.tax7i.classification.dto.ClassificationResult;
import com.ssafy.tax7i.classification.entity.MccTaxRule;
import com.ssafy.tax7i.classification.entity.MerchantKeywordMapping;
import com.ssafy.tax7i.classification.entity.TaxLimit;
import com.ssafy.tax7i.classification.repository.MerchantKeywordMappingRepository;
import com.ssafy.tax7i.classification.repository.MerchantRepository;
import com.ssafy.tax7i.classification.repository.TaxLimitRepository;
import com.ssafy.tax7i.ai.service.AiClassificationService;
import com.ssafy.tax7i.tax.service.TaxParameterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ClassificationServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private ClassificationCacheService classificationCacheService;
    @Mock private MerchantKeywordMappingRepository keywordMappingRepository;
    @Mock private EntertainmentLimitService entertainmentLimitService;
    @Mock private TaxLimitRepository taxLimitRepository;
    @Mock private AiClassificationService aiClassificationService;
    @Mock private TaxParameterService taxParameterService;
    @Mock private CategoryLearningService categoryLearningService;

    @InjectMocks
    private TaxClassificationService taxClassificationService;

    // ───────────── helpers ─────────────

    /** @NoArgsConstructor(PROTECTED) 우회 — reflection으로 MccTaxRule 인스턴스 생성 */
    private MccTaxRule newMccTaxRule() {
        try {
            java.lang.reflect.Constructor<MccTaxRule> constructor =
                    MccTaxRule.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** @NoArgsConstructor(PROTECTED) 우회 — reflection으로 MerchantKeywordMapping 인스턴스 생성 */
    private MerchantKeywordMapping newMerchantKeywordMapping() {
        try {
            java.lang.reflect.Constructor<MerchantKeywordMapping> constructor =
                    MerchantKeywordMapping.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Tier A MccTaxRule — MCC만으로 자동 확정. requiresUserConfirm = false */
    private MccTaxRule tierARule(String mcc, String taxCategory, int confidence) {
        MccTaxRule rule = newMccTaxRule();
        setField(rule, "mcc", mcc);
        setField(rule, "tier", "A");
        setField(rule, "conditionExpr", "MCC_EXACT");
        setField(rule, "taxCategory", taxCategory);
        setField(rule, "vatDeductible", "공제");
        setField(rule, "legalBasis", "소득세법§27");
        setField(rule, "remark", null);
        setField(rule, "confidence", confidence);
        setField(rule, "requiresUserConfirm", false);
        return rule;
    }

    /** Tier B MccTaxRule — 금액 조건 */
    private MccTaxRule tierBAmountRule(String mcc, String conditionExpr, long threshold,
                                       String taxCategory, int confidence) {
        MccTaxRule rule = newMccTaxRule();
        setField(rule, "mcc", mcc);
        setField(rule, "tier", "B");
        setField(rule, "conditionExpr", conditionExpr);
        setField(rule, "taxCategory", taxCategory);
        setField(rule, "vatDeductible", "공제");
        setField(rule, "legalBasis", null);
        setField(rule, "remark", null);
        setField(rule, "amountThreshold", threshold);
        setField(rule, "confidence", confidence);
        setField(rule, "requiresUserConfirm", false);
        return rule;
    }

    /** Tier B MccTaxRule — 가맹점명 키워드 조건 */
    private MccTaxRule tierBKeywordRule(String mcc, String keywords, String taxCategory, int confidence) {
        MccTaxRule rule = newMccTaxRule();
        setField(rule, "mcc", mcc);
        setField(rule, "tier", "B");
        setField(rule, "conditionExpr", "MERCHANT_LIKE:" + keywords);
        setField(rule, "taxCategory", taxCategory);
        setField(rule, "vatDeductible", "공제");
        setField(rule, "legalBasis", null);
        setField(rule, "remark", null);
        setField(rule, "confidence", confidence);
        setField(rule, "requiresUserConfirm", false);
        return rule;
    }

    /** Tier B MccTaxRule — 사용자 확인 조건 (DEFAULT 등) */
    private MccTaxRule tierBDefaultRule(String mcc, String conditionExpr, String taxCategory,
                                        boolean requiresUserConfirm, int confidence) {
        MccTaxRule rule = newMccTaxRule();
        setField(rule, "mcc", mcc);
        setField(rule, "tier", "B");
        setField(rule, "conditionExpr", conditionExpr);
        setField(rule, "taxCategory", taxCategory);
        setField(rule, "vatDeductible", "확인필요");
        setField(rule, "legalBasis", null);
        setField(rule, "remark", "사용자 확인 필요");
        setField(rule, "confidence", confidence);
        setField(rule, "requiresUserConfirm", requiresUserConfirm);
        return rule;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            // 부모 클래스까지 탐색
            try {
                java.lang.reflect.Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ───────────── classify — Tier A (MCC 직접 매칭) ─────────────

    @Test
    void classify_MCC매칭_CONFIRMED() {
        // mcc=5045(컴퓨터 주변기기)는 Tier A 소모품비로 확정 매칭
        MccTaxRule rule = tierARule("5045", "소모품비", 95);
        given(classificationCacheService.getMccRules("5045")).willReturn(List.of(rule));

        ClassificationRequest request = new ClassificationRequest(
                "11번가", "5045", 80000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.CONFIRMED);
        assertThat(result.taxCategory()).isEqualTo("소모품비");
        assertThat(result.confidenceScore()).isEqualTo(95);
        assertThat(result.entertainmentLimit()).isNull();
    }

    @Test
    void classify_MCC매칭_requiresUserConfirm_true이면_RECOMMENDED() {
        // requiresUserConfirm=true인 Tier A 룰은 RECOMMENDED로 반환
        MccTaxRule rule = tierARule("4813", "통신비", 90);
        setField(rule, "requiresUserConfirm", true);
        given(classificationCacheService.getMccRules("4813")).willReturn(List.of(rule));

        ClassificationRequest request = new ClassificationRequest(
                "KT", "4813", 55000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
        assertThat(result.taxCategory()).isEqualTo("통신비");
    }

    // ───────────── classify — Tier B 금액 조건 ─────────────

    @Test
    void classify_금액조건_AMOUNT_LT_RECOMMENDED() {
        // mcc=5045 Tier B: 금액 < 1,000,000 → 소모품비 RECOMMENDED
        MccTaxRule amountRule = tierBAmountRule("5045", "AMOUNT_LT_1000000", 1_000_000L, "소모품비", 75);
        given(classificationCacheService.getMccRules("5045")).willReturn(List.of(amountRule));

        ClassificationRequest request = new ClassificationRequest(
                "11번가", "5045", 500_000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
        assertThat(result.taxCategory()).isEqualTo("소모품비");
        assertThat(result.confidenceScore()).isEqualTo(75);
    }

    @Test
    void classify_금액조건_AMOUNT_GTE_RECOMMENDED() {
        // 금액 >= 1,000,000 → 고정자산 매입 RECOMMENDED
        MccTaxRule amountRule = tierBAmountRule("5045", "AMOUNT_GTE_1000000", 1_000_000L, "고정자산 매입", 70);
        given(classificationCacheService.getMccRules("5045")).willReturn(List.of(amountRule));

        ClassificationRequest request = new ClassificationRequest(
                "11번가", "5045", 2_000_000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
        assertThat(result.taxCategory()).isEqualTo("고정자산 매입");
    }

    @Test
    void classify_amount_null_금액조건스킵_폴백처리() {
        // amount=null이면 Tier B 금액조건을 건너뛰고 폴백(NEEDS_CONFIRMATION)으로 처리
        MccTaxRule amountRule = tierBAmountRule("5045", "AMOUNT_LT_1000000", 1_000_000L, "소모품비", 75);
        given(classificationCacheService.getMccRules("5045")).willReturn(List.of(amountRule));
        given(keywordMappingRepository.findByMcc("5045")).willReturn(List.of());

        ClassificationRequest request = new ClassificationRequest(
                "11번가", "5045", null, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        // amount가 null이면 금액조건 미적용, 폴백으로 NEEDS_CONFIRMATION 반환
        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.NEEDS_CONFIRMATION);
    }

    // ───────────── classify — Tier B 가맹점명 키워드 매칭 ─────────────

    @Test
    void classify_가맹점명키워드매칭_RECOMMENDED() {
        // conditionExpr에 MERCHANT_LIKE 포함 → 키워드 매칭으로 RECOMMENDED
        MccTaxRule keywordRule = tierBKeywordRule("5817", "AWS,GCP,AZURE", "지급수수료", 85);
        given(classificationCacheService.getMccRules("5817")).willReturn(List.of(keywordRule));

        ClassificationRequest request = new ClassificationRequest(
                "AWS 서울 리전", "5817", 150000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
        assertThat(result.taxCategory()).isEqualTo("지급수수료");
        assertThat(result.confidenceScore()).isEqualTo(85);
    }

    @Test
    void classify_키워드테이블매칭_RECOMMENDED() {
        // MERCHANT_LIKE 룰 없이 merchant_keyword_mapping 테이블에서 매칭
        MccTaxRule fallbackRule = tierBDefaultRule("5817", "DEFAULT", "지급수수료", false, 55);
        given(classificationCacheService.getMccRules("5817")).willReturn(List.of(fallbackRule));

        MerchantKeywordMapping mapping = newMerchantKeywordMapping();
        setField(mapping, "keyword", "GITHUB");
        setField(mapping, "mcc", "5817");
        setField(mapping, "taxCategory", "지급수수료");
        setField(mapping, "confidence", 80);
        given(keywordMappingRepository.findByMcc("5817")).willReturn(List.of(mapping));

        ClassificationRequest request = new ClassificationRequest(
                "GITHUB INC", "5817", 12000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
        assertThat(result.taxCategory()).isEqualTo("지급수수료");
    }

    // ───────────── classify — NEEDS_CONFIRMATION ─────────────

    @Test
    void classify_매칭없음_NEEDS_CONFIRMATION() {
        // Tier A/B 어느 것도 매칭 안 됨 → 폴백 NEEDS_CONFIRMATION
        MccTaxRule fallbackRule = tierBDefaultRule("9999", "DEFAULT", "기타 경비", true, 30);
        given(classificationCacheService.getMccRules("9999")).willReturn(List.of(fallbackRule));

        ClassificationRequest request = new ClassificationRequest(
                "알수없는가맹점", "9999", 30000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.NEEDS_CONFIRMATION);
    }

    @Test
    void classify_MCC결정불가_NEEDS_CONFIRMATION() {
        // mcc null이고 가맹점명으로도 MCC 조회 실패 → NEEDS_CONFIRMATION 즉시 반환
        given(classificationCacheService.getMerchantByName("알수없는가맹점")).willReturn(Optional.empty());
        given(merchantRepository.findByMerchantNameContainedIn(anyString())).willReturn(List.of());

        ClassificationRequest request = new ClassificationRequest(
                "알수없는가맹점", null, 30000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.NEEDS_CONFIRMATION);
        assertThat(result.taxCategory()).isEqualTo("기타 경비");
    }

    @Test
    void classify_MCC에룰없음_NEEDS_CONFIRMATION() {
        // MCC는 결정됐지만 해당 MCC에 룰이 없는 경우
        given(classificationCacheService.getMccRules("0000")).willReturn(List.of());

        ClassificationRequest request = new ClassificationRequest(
                "특수가맹점", "0000", 10000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.NEEDS_CONFIRMATION);
    }

    // ───────────── classify — 사용자 확인 조건 ─────────────

    @Test
    void classify_거래처동행_null이면_NEEDS_CONFIRMATION() {
        // isClientAccompanied=null → 사용자 확인 요청
        MccTaxRule rule = tierBDefaultRule("5812", "거래처동행", "접대비", false, 70);
        given(classificationCacheService.getMccRules("5812")).willReturn(List.of(rule));

        ClassificationRequest request = new ClassificationRequest(
                "한식당", "5812", 50000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.NEEDS_CONFIRMATION);
    }

    @Test
    void classify_거래처동행_true이면_접대비_RECOMMENDED() {
        // isClientAccompanied=true → 접대비 RECOMMENDED
        MccTaxRule rule = tierBDefaultRule("5812", "거래처동행", "접대비", false, 70);
        given(classificationCacheService.getMccRules("5812")).willReturn(List.of(rule));
        given(taxLimitRepository.findByTaxCategoryAndLimitType("접대비", "연간기본한도"))
                .willReturn(Optional.of(mockTaxLimit(12_000_000L)));
        given(entertainmentLimitService.getUsedEntertainmentAmount(1L)).willReturn(1_000_000L);

        ClassificationRequest request = new ClassificationRequest(
                "한식당", "5812", 50000L, null, true, 1L, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
        assertThat(result.taxCategory()).isEqualTo("접대비");
    }

    @Test
    void classify_거래처동행_false이면_경비불인정() {
        // isClientAccompanied=false → 1인 사업자 본인 식대는 가사경비 처리
        MccTaxRule rule = tierBDefaultRule("5812", "거래처동행", "접대비", false, 70);
        given(classificationCacheService.getMccRules("5812")).willReturn(List.of(rule));

        ClassificationRequest request = new ClassificationRequest(
                "한식당", "5812", 15000L, null, false, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.taxCategory()).isEqualTo("경비불인정");
        assertThat(result.confidence()).isEqualTo(ClassificationResult.Confidence.RECOMMENDED);
    }

    // ───────────── classify — 접대비 한도 체크 ─────────────

    @Test
    void classify_접대비_한도미초과_entertainmentLimit첨부() {
        // 접대비로 분류될 때 EntertainmentLimitInfo가 결과에 첨부됨
        MccTaxRule rule = tierARule("5812", "접대비", 90);
        given(classificationCacheService.getMccRules("5812")).willReturn(List.of(rule));
        given(taxLimitRepository.findByTaxCategoryAndLimitType("접대비", "연간기본한도"))
                .willReturn(Optional.of(mockTaxLimit(12_000_000L)));
        given(entertainmentLimitService.getUsedEntertainmentAmount(1L)).willReturn(3_000_000L);

        ClassificationRequest request = new ClassificationRequest(
                "스테이크 하우스", "5812", 200000L, null, null, 1L, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.taxCategory()).isEqualTo("접대비");
        assertThat(result.entertainmentLimit()).isNotNull();
        assertThat(result.entertainmentLimit().annualLimit()).isEqualTo(12_000_000L);
        assertThat(result.entertainmentLimit().usedAmount()).isEqualTo(3_000_000L);
        assertThat(result.entertainmentLimit().remainingAmount()).isEqualTo(9_000_000L);
        assertThat(result.entertainmentLimit().isOverLimit()).isFalse();
    }

    @Test
    void classify_접대비_한도초과_isOverLimit_true() {
        // 접대비 사용액이 연간 한도를 초과한 경우
        MccTaxRule rule = tierARule("5812", "접대비", 90);
        given(classificationCacheService.getMccRules("5812")).willReturn(List.of(rule));
        given(taxLimitRepository.findByTaxCategoryAndLimitType("접대비", "연간기본한도"))
                .willReturn(Optional.of(mockTaxLimit(12_000_000L)));
        given(entertainmentLimitService.getUsedEntertainmentAmount(1L)).willReturn(13_000_000L);

        ClassificationRequest request = new ClassificationRequest(
                "고급 일식당", "5812", 500000L, null, null, 1L, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.entertainmentLimit()).isNotNull();
        assertThat(result.entertainmentLimit().isOverLimit()).isTrue();
        assertThat(result.entertainmentLimit().remainingAmount()).isEqualTo(0L);
    }

    @Test
    void classify_접대비_userId_null이면_entertainmentLimit없음() {
        // userId가 없으면 접대비 한도 체크를 건너뜀
        MccTaxRule rule = tierARule("5812", "접대비", 90);
        given(classificationCacheService.getMccRules("5812")).willReturn(List.of(rule));

        ClassificationRequest request = new ClassificationRequest(
                "스테이크 하우스", "5812", 200000L, null, null, null, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.taxCategory()).isEqualTo("접대비");
        assertThat(result.entertainmentLimit()).isNull();
    }

    @Test
    void classify_접대비_taxLimit_DB없을때_기본값_12000000사용() {
        // tax_limit 테이블에 데이터 없으면 기본값 1,200만원 적용
        MccTaxRule rule = tierARule("5812", "접대비", 90);
        given(classificationCacheService.getMccRules("5812")).willReturn(List.of(rule));
        given(taxLimitRepository.findByTaxCategoryAndLimitType("접대비", "연간기본한도"))
                .willReturn(Optional.empty()); // DB에 데이터 없음
        given(taxParameterService.getEntertainmentLimit(any(Integer.class))).willReturn(12_000_000L);
        given(entertainmentLimitService.getUsedEntertainmentAmount(1L)).willReturn(500_000L);

        ClassificationRequest request = new ClassificationRequest(
                "거래처 식사", "5812", 100000L, null, null, 1L, null);

        ClassificationResult result = taxClassificationService.classify(request);

        assertThat(result.entertainmentLimit().annualLimit()).isEqualTo(12_000_000L);
    }

    // ───────────── helpers ─────────────

    private TaxLimit mockTaxLimit(long limitAmount) {
        try {
            java.lang.reflect.Constructor<TaxLimit> constructor =
                    TaxLimit.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            TaxLimit taxLimit = constructor.newInstance();
            setField(taxLimit, "limitAmount", limitAmount);
            return taxLimit;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
